package com.giftnova.controller;

import com.giftnova.repository.UpcomingEventRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager")
public class ManagerApprovalController {

    private final UpcomingEventRepository eventRepo;

    public ManagerApprovalController(UpcomingEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @GetMapping("/{token}/approve")
    public String approve(@PathVariable String token, Model model) {
        return eventRepo.findByManagerToken(token).map(event -> {
            event.setStatus("APPROVED");
            eventRepo.save(event);
            model.addAttribute("employeeName", event.getEmployeeName());
            model.addAttribute("eventType",    event.getEventType().getLabel());
            model.addAttribute("action",       "approved");
            return "manager/decision";
        }).orElse("manager/invalid");
    }

    @GetMapping("/{token}/reject")
    public String reject(@PathVariable String token, Model model) {
        return eventRepo.findByManagerToken(token).map(event -> {
            event.setStatus("REJECTED");
            eventRepo.save(event);
            model.addAttribute("employeeName", event.getEmployeeName());
            model.addAttribute("eventType",    event.getEventType().getLabel());
            model.addAttribute("action",       "rejected");
            return "manager/decision";
        }).orElse("manager/invalid");
    }
}
