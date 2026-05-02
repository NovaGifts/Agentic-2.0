package com.giftnova.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftnova.model.*;
import com.giftnova.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.*;

/**
 * Phase 5: AI Recommendation Engine
 *
 * Workflow:
 *   1. Build a structured prompt with employee/event/policy/catalog context
 *   2. Call Claude API — AI recommends gifts and writes a message draft
 *   3. Deterministic policy engine enforces budget + approval rules
 *      (AI can suggest; it cannot approve spending)
 *   4. Save and return the GiftRecommendation
 */
@Service
public class AiRecommendationService {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GiftCatalogService catalogService;
    private final GiftPolicyRepository policyRepo;
    private final GiftRecommendationRepository recRepo;
    private final UpcomingEventRepository eventRepo;

    public AiRecommendationService(RestTemplate restTemplate,
                                    ObjectMapper objectMapper,
                                    GiftCatalogService catalogService,
                                    GiftPolicyRepository policyRepo,
                                    GiftRecommendationRepository recRepo,
                                    UpcomingEventRepository eventRepo) {
        this.restTemplate   = restTemplate;
        this.objectMapper   = objectMapper;
        this.catalogService = catalogService;
        this.policyRepo     = policyRepo;
        this.recRepo        = recRepo;
        this.eventRepo      = eventRepo;
    }

    /**
     * Generates (or regenerates) an AI recommendation for the given event.
     * Deletes any existing recommendation for the same event before saving the new one.
     */
    public GiftRecommendation generate(Long companyId, Long eventId) throws Exception {
        // Fetch event and its policy
        UpcomingEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        GiftPolicy policy = policyRepo
                .findByCompanyIdAndEventType(companyId, event.getEventType())
                .orElseThrow(() -> new IllegalStateException("No policy found for " + event.getEventType()));

        // Delete stale recommendation if it exists
        recRepo.findByEventId(eventId).ifPresent(recRepo::delete);

        // Step 1 — Ask local LLM for gift suggestions and message draft
        String prompt   = buildPrompt(event, policy);
        String aiJson   = callAi(prompt, policy.getBudgetLimit());

        // Step 2 — Parse AI output and enforce policy deterministically
        GiftRecommendation rec = parseResponse(aiJson, event, policy);
        rec.setEventId(eventId);
        rec.setCompanyId(companyId);
        return recRepo.save(rec);
    }

    public Optional<GiftRecommendation> findByEvent(Long eventId) {
        return recRepo.findByEventId(eventId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    // Builds the prompt giving Claude full context: event, employee, policy, catalog
    private String buildPrompt(UpcomingEvent event, GiftPolicy policy) throws Exception {
        // Send only 10 gifts within budget to keep the prompt small enough for local models
        java.math.BigDecimal limit = policy.getBudgetLimit();
        List<java.util.Map<String, Object>> shortList = catalogService.getAll().stream()
                .filter(g -> new java.math.BigDecimal(g.get("price").toString()).compareTo(limit) <= 0)
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        String catalogJson = objectMapper.writeValueAsString(shortList);

        return """
                You are a gift recommendation engine. Return ONLY valid JSON, no extra text.

                Employee: %s | Event: %s | Budget limit: $%s

                Pick 3 gifts from this catalog:
                %s

                Return this exact JSON structure:
                {"event_type":"%s","recommended_budget":%s,"recommended_gift_ids":["id1","id2","id3"],"message_draft":"...","reasoning":"...","risk_flags":[]}
                """.formatted(
                event.getEmployeeName(),
                event.getEventType().getLabel(),
                policy.getBudgetLimit(),
                catalogJson,
                event.getEventType().name().toLowerCase(),
                policy.getBudgetLimit()
        );
    }

    // Calls the local LLM (Ollama) and returns the raw text response
    @SuppressWarnings("unchecked")
    private String callAi(String prompt, BigDecimal budgetLimit) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return mockResponse(budgetLimit);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
        if (response == null) throw new IllegalStateException("Empty response from local LLM");

        // OpenAI-compatible response: { "choices": [{"message": {"content": "..."}}] }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // Parses Claude's JSON output, then enforces policy rules deterministically
    private GiftRecommendation parseResponse(String rawText, UpcomingEvent event,
                                              GiftPolicy policy) throws Exception {
        String json = extractJson(rawText);
        JsonNode node = objectMapper.readTree(json);

        JsonNode giftIdsNode = node.get("recommended_gift_ids");
        JsonNode budgetNode  = node.get("recommended_budget");
        if (giftIdsNode == null || giftIdsNode.isNull()) {
            throw new IllegalStateException("AI response missing 'recommended_gift_ids'");
        }
        if (budgetNode == null || budgetNode.isNull()) {
            throw new IllegalStateException("AI response missing 'recommended_budget'");
        }

        GiftRecommendation rec = new GiftRecommendation();
        rec.setRecommendedGiftIds(giftIdsNode.toString());
        rec.setMessageDraft(node.hasNonNull("message_draft") ? node.get("message_draft").asText() : "");
        rec.setReasoning(node.hasNonNull("reasoning")        ? node.get("reasoning").asText()      : "");
        rec.setRiskFlags(node.hasNonNull("risk_flags")       ? node.get("risk_flags").toString()   : "[]");

        BigDecimal budget;
        try {
            budget = new BigDecimal(budgetNode.asText());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("AI response has invalid 'recommended_budget': " + budgetNode.asText());
        }
        rec.setRecommendedBudget(budget);

        // ── DETERMINISTIC POLICY ENFORCEMENT ──────────────────────────────────
        // AI cannot set policyStatus or approvalRequired — only this code can.

        List<String> flags = new ArrayList<>();

        // Budget compliance check
        if (budget.compareTo(policy.getBudgetLimit()) > 0) {
            rec.setPolicyStatus("non_compliant");
            flags.add("Recommended budget ($" + budget + ") exceeds policy limit ($" + policy.getBudgetLimit() + ")");
        } else {
            rec.setPolicyStatus("compliant");
        }

        // Approval check — threshold null means approval is always required when enabled
        boolean needsApproval = policy.isApprovalRequired() &&
                (policy.getApprovalThreshold() == null ||
                 budget.compareTo(policy.getApprovalThreshold()) > 0);
        rec.setApprovalRequired(needsApproval);

        // Policy engine flags replace AI flags — policy rules are authoritative
        if (!flags.isEmpty()) {
            rec.setRiskFlags(objectMapper.writeValueAsString(flags));
        }

        return rec;
    }

    // Extracts the first { ... } block from text, stripping markdown code fences if present
    private String extractJson(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence    = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}') + 1;
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object found in AI response");
        }
        return cleaned.substring(start, end);
    }

    // Mock response used when ai.api.url is blank — picks real catalog gifts within the budget
    private String mockResponse(BigDecimal budgetLimit) {
        List<Map<String, Object>> eligible = catalogService.getAll().stream()
                .filter(g -> new BigDecimal(g.get("price").toString()).compareTo(budgetLimit) <= 0)
                .limit(3)
                .collect(java.util.stream.Collectors.toList());

        if (eligible.isEmpty()) {
            eligible = catalogService.getAll().stream().limit(3)
                    .collect(java.util.stream.Collectors.toList());
        }

        List<String> ids = eligible.stream()
                .map(g -> "\"" + g.get("id") + "\"")
                .collect(java.util.stream.Collectors.toList());
        String idsJson = "[" + String.join(",", ids) + "]";

        BigDecimal budget = eligible.stream()
                .map(g -> new BigDecimal(g.get("price").toString()))
                .max(BigDecimal::compareTo)
                .orElse(budgetLimit);

        return """
                {
                  "event_type": "work_anniversary",
                  "recommended_budget": %s,
                  "recommended_gift_ids": %s,
                  "message_draft": "Wishing you a wonderful celebration! Your dedication and contributions mean so much to the team.",
                  "reasoning": "Selected gifts within the policy budget limit of $%s. These options are versatile and well-suited for the occasion.",
                  "risk_flags": []
                }
                """.formatted(budget, idsJson, budgetLimit);
    }
}
