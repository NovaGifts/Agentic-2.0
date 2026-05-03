package com.giftnova.service;

import com.giftnova.model.*;
import com.giftnova.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs every day at 9 AM and sends gift links to employees whose event falls on today.
 * Events are prepared (AI rec + tokens) when the CSV is uploaded; this service
 * does the actual email delivery on the right day.
 */
@Service
public class GiftSchedulerService {

    private final UpcomingEventRepository eventRepo;
    private final EmployeeRepository      employeeRepo;
    private final CompanyRepository       companyRepo;
    private final GiftRecommendationRepository recRepo;
    private final AiRecommendationService aiService;
    private final EmailService            emailService;

    public GiftSchedulerService(UpcomingEventRepository eventRepo,
                                 EmployeeRepository employeeRepo,
                                 CompanyRepository companyRepo,
                                 GiftRecommendationRepository recRepo,
                                 AiRecommendationService aiService,
                                 EmailService emailService) {
        this.eventRepo    = eventRepo;
        this.employeeRepo = employeeRepo;
        this.companyRepo  = companyRepo;
        this.recRepo      = recRepo;
        this.aiService    = aiService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendTodaysGiftLinks() {
        LocalDate today = LocalDate.now();
        List<UpcomingEvent> todaysEvents = eventRepo.findByEventDateAndStatus(today, "NEEDS_REVIEW");

        for (UpcomingEvent event : todaysEvents) {
            try {
                companyRepo.findById(event.getCompanyId()).ifPresent(company -> {
                    // Generate rec if somehow missing
                    GiftRecommendation rec = recRepo.findByEventId(event.getId()).orElseGet(() -> {
                        try {
                            return aiService.generate(event.getCompanyId(), event.getId());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    if (event.getRecipientToken() == null)
                        event.setRecipientToken(java.util.UUID.randomUUID().toString());
                    if (event.getManagerToken() == null)
                        event.setManagerToken(java.util.UUID.randomUUID().toString());
                    event.setStatus("LINK_SENT");
                    eventRepo.save(event);

                    employeeRepo.findById(event.getEmployeeId()).ifPresent(emp -> {
                        emailService.sendRecommendationToEmployee(
                            emp.getEmail(), emp.getName(), company.getName(),
                            event.getEventType().getLabel(), rec.getMessageDraft(),
                            event.getRecipientToken());

                        String managerEmail = emailService.resolveManagerEmail(emp, company);
                        String managerName  = emailService.resolveManagerName(emp, company);
                        emailService.sendManagerFyi(managerEmail, managerName,
                            event.getEmployeeName(), company.getName(), event.getEventType().getLabel());
                        if (!managerEmail.equals(company.getAdminEmail())) {
                            emailService.sendHrFyi(company.getAdminEmail(), company.getAdminName(),
                                event.getEmployeeName(), event.getEventType().getLabel());
                        }
                    });
                });
            } catch (Exception e) {
                System.err.println("Scheduler failed for event " + event.getId() + ": " + e.getMessage());
            }
        }
    }
}
