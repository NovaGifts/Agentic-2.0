package com.giftnova.controller;

import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 4: Lifecycle Calendar
 * Shows all upcoming gift events grouped by month with status management.
 *
 * Routes:
 *   GET  /companies/{id}/calendar                    → calendar list view
 *   POST /companies/{id}/calendar/events/{eid}/status → update event status
 */
@Controller
@RequestMapping("/companies/{companyId}/calendar")
public class CalendarController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("MMM d");

    private final UpcomingEventRepository eventRepo;
    private final CompanyService companyService;

    public CalendarController(UpcomingEventRepository eventRepo, CompanyService companyService) {
        this.eventRepo      = eventRepo;
        this.companyService = companyService;
    }

    // Groups events by "Month Year" so the template can render month sections
    @GetMapping
    public String show(@PathVariable Long companyId, Model model) {
        List<UpcomingEvent> all = eventRepo.findByCompanyIdOrderByEventDate(companyId);

        // LinkedHashMap preserves month order (Jan → Feb → Mar...)
        Map<String, List<UpcomingEvent>> byMonth = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventDate().format(MONTH_FMT),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("company",      companyService.findById(companyId));
        model.addAttribute("eventsByMonth", byMonth);
        model.addAttribute("totalEvents",  all.size());
        model.addAttribute("dayFmt",       DAY_FMT);
        return "calendar/index";
    }

    // Updates a single event's status (Needs Review → Approval Needed → Approved, etc.)
    @PostMapping("/events/{eventId}/status")
    public String updateStatus(@PathVariable Long companyId,
                                @PathVariable Long eventId,
                                @RequestParam String status) {
        UpcomingEvent ev = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ev.setStatus(status);
        eventRepo.save(ev);
        return "redirect:/companies/" + companyId + "/calendar";
    }
}
