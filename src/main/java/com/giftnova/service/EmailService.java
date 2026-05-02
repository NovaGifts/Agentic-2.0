package com.giftnova.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the recipient gift-link email to the employee.
 * Safe to use when SMTP is not configured — isConfigured() returns false
 * and sendGiftLink() is a no-op, so HR copies the link manually instead.
 */
@Service
public class EmailService {

    // Optional — Spring won't inject this bean if spring.mail.host is blank
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${mail.from:noreply@giftnova.app}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public boolean isConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank();
    }

    public void sendGiftLink(String toEmail, String employeeName, String companyName,
                              String eventType, String recipientUrl) {
        if (!isConfigured()) return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject(companyName + " has a gift for you! 🎁");
        msg.setText(
            "Hi " + employeeName + ",\n\n" +
            companyName + " wants to celebrate your " + eventType + "!\n\n" +
            "Choose your gift here:\n" + recipientUrl + "\n\n" +
            "This link is personal to you — please don't share it.\n\n" +
            "– The GiftNova Team"
        );
        mailSender.send(msg);
    }

    public void sendRecommendationToEmployee(String toEmail, String employeeName,
                                              String companyName, String eventType,
                                              String messageDraft,
                                              java.util.List<java.util.Map<String, Object>> gifts,
                                              String recipientToken) {
        if (!isConfigured()) return;

        StringBuilder giftList = new StringBuilder();
        int i = 1;
        for (java.util.Map<String, Object> gift : gifts) {
            giftList.append(i++).append(". ")
                    .append(gift.get("name")).append(" — $").append(gift.get("price"))
                    .append("\n");
        }

        String selectUrl = baseUrl + "/gift/" + recipientToken;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject(companyName + " — Your gift for " + eventType);
        msg.setText(
            "Hi " + employeeName + ",\n\n" +
            messageDraft + "\n\n" +
            "Here are your gift options:\n" + giftList + "\n" +
            "👉 Choose your gift here (no login needed):\n" + selectUrl + "\n\n" +
            "This link is personal to you — please don't share it.\n\n" +
            "– " + companyName
        );
        mailSender.send(msg);
    }

    public void sendManagerFyi(String managerEmail, String managerName,
                                String employeeName, String companyName, String eventType) {
        if (!isConfigured()) return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(managerEmail);
        msg.setSubject("[GiftNova] Gift email sent to " + employeeName);
        msg.setText(
            "Hi " + managerName + ",\n\n" +
            "A gift selection email has been sent to " + employeeName +
            " for their " + eventType + ".\n\n" +
            "You will receive another email once they make their selection — " +
            "that email will include approve and reject links.\n\n" +
            "– GiftNova"
        );
        mailSender.send(msg);
    }

    public void sendApprovalRequestToManager(String managerEmail, String managerName,
                                              String employeeName, String companyName,
                                              String eventType, String messageDraft,
                                              java.util.List<java.util.Map<String, Object>> gifts,
                                              java.math.BigDecimal budget,
                                              String managerToken) {
        if (!isConfigured()) return;

        StringBuilder giftList = new StringBuilder();
        for (java.util.Map<String, Object> gift : gifts) {
            giftList.append("• ").append(gift.get("name"))
                    .append(" — $").append(gift.get("price")).append("\n");
        }

        String approveUrl = baseUrl + "/manager/" + managerToken + "/approve";
        String rejectUrl  = baseUrl + "/manager/" + managerToken + "/reject";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(managerEmail);
        msg.setSubject("[GiftNova] Approval needed — " + employeeName + "'s " + eventType);
        msg.setText(
            "Hi " + managerName + ",\n\n" +
            "A gift recommendation for " + employeeName + " requires your approval.\n\n" +
            "Event: " + eventType + "\n" +
            "Budget: $" + budget + "\n\n" +
            "Recommended gifts:\n" + giftList + "\n" +
            "Message draft:\n" + messageDraft + "\n\n" +
            "✅ APPROVE:  " + approveUrl + "\n" +
            "❌ REJECT:   " + rejectUrl + "\n\n" +
            "Just click the link — no login required.\n\n" +
            "– GiftNova"
        );
        mailSender.send(msg);
    }
}
