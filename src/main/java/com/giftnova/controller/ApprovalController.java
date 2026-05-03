package com.giftnova.controller;

import com.giftnova.model.ApprovalItem;
import com.giftnova.service.ApprovalService;
import com.giftnova.service.CompanyService;
import com.giftnova.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/companies/{companyId}/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final CompanyService  companyService;
    private final EmailService    emailService;

    public ApprovalController(ApprovalService approvalService, CompanyService companyService,
                               EmailService emailService) {
        this.approvalService = approvalService;
        this.companyService  = companyService;
        this.emailService    = emailService;
    }

    @GetMapping
    public String queue(@PathVariable Long companyId, Model model) {
        List<ApprovalItem> queue = approvalService.getQueue(companyId);

        long selectionCount = queue.stream().filter(i -> i.getRecommendation().getSelectedGiftId() == null).count();
        long approvedCount  = queue.stream().filter(i -> "APPROVED".equals(i.getEvent().getStatus())).count();
        long rejectedCount  = queue.stream().filter(i -> "REJECTED".equals(i.getEvent().getStatus())).count();

        model.addAttribute("company",        companyService.findById(companyId));
        model.addAttribute("queue",          queue);
        model.addAttribute("selectionCount", selectionCount);
        model.addAttribute("approvedCount",  approvedCount);
        model.addAttribute("rejectedCount",  rejectedCount);
        return "approvals/index";
    }

    @PostMapping("/{eventId}/approve")
    public String approve(@PathVariable Long companyId,
                          @PathVariable Long eventId,
                          RedirectAttributes ra) {
        approvalService.approve(eventId);
        ra.addFlashAttribute("toast", "Gift approved.");
        return "redirect:/companies/" + companyId + "/approvals";
    }

    @PostMapping("/{eventId}/reject")
    public String reject(@PathVariable Long companyId,
                         @PathVariable Long eventId,
                         RedirectAttributes ra) {
        approvalService.reject(eventId);
        ra.addFlashAttribute("toast", "Recommendation rejected.");
        return "redirect:/companies/" + companyId + "/approvals";
    }

    @PostMapping("/{eventId}/review")
    public String review(@PathVariable Long companyId,
                         @PathVariable Long eventId,
                         RedirectAttributes ra) {
        approvalService.requestReview(eventId);
        ra.addFlashAttribute("toast", "Sent for manager review.");
        return "redirect:/companies/" + companyId + "/approvals";
    }

    @GetMapping("/{eventId}/edit")
    public String editForm(@PathVariable Long companyId,
                           @PathVariable Long eventId,
                           Model model) {
        ApprovalItem item = approvalService.getItem(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        model.addAttribute("company", companyService.findById(companyId));
        model.addAttribute("item", item);
        return "approvals/edit";
    }

    @PostMapping("/{eventId}/edit")
    public String saveEdit(@PathVariable Long companyId,
                           @PathVariable Long eventId,
                           @RequestParam String messageDraft,
                           @RequestParam BigDecimal budget,
                           RedirectAttributes ra) {
        approvalService.updateDraft(eventId, messageDraft, budget);
        ra.addFlashAttribute("toast", "Draft updated.");
        return "redirect:/companies/" + companyId + "/approvals";
    }

    // HR sends the gift-choice link to the employee (generates token + optional email)
    @PostMapping("/{eventId}/send-link")
    public String sendLink(@PathVariable Long companyId,
                           @PathVariable Long eventId,
                           RedirectAttributes ra) {
        String url = approvalService.sendLink(companyId, eventId);
        if (url != null) {
            ra.addFlashAttribute("recipientLink", url);
            String msg = emailService.isConfigured()
                    ? "Email sent to employee! Link also shown below."
                    : "Link ready — copy it below to share with the employee (SMTP not configured).";
            ra.addFlashAttribute("toast", msg);
        }
        return "redirect:/companies/" + companyId + "/approvals";
    }

    // HR confirms the employee's chosen gift → status becomes SENT
    @PostMapping("/{eventId}/approve-selection")
    public String approveSelection(@PathVariable Long companyId,
                                   @PathVariable Long eventId,
                                   RedirectAttributes ra) {
        approvalService.approveSelection(eventId);
        ra.addFlashAttribute("toast", "Gift confirmed! The employee's selection has been approved.");
        return "redirect:/companies/" + companyId + "/approvals";
    }

}
