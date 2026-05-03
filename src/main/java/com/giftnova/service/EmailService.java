package com.giftnova.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

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
                                              String recipientToken) {
        if (!isConfigured()) return;

        String selectUrl = baseUrl + "/gift/" + recipientToken;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject(companyName + " — Your gift for " + eventType);
        msg.setText(
            "Hi " + employeeName + ",\n\n" +
            messageDraft + "\n\n" +
            "We have a surprise gift waiting for you! Click below to reveal and choose your gift:\n" +
            selectUrl + "\n\n" +
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

    public void sendHrFyi(String hrEmail, String hrName,
                           String employeeName, String eventType) {
        if (!isConfigured()) return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(hrEmail);
        msg.setSubject("[GiftNova] Gift link sent to " + employeeName);
        msg.setText(
            "Hi " + hrName + ",\n\n" +
            "A gift selection link has been sent to " + employeeName +
            " for their " + eventType + ".\n\n" +
            "– GiftNova"
        );
        mailSender.send(msg);
    }

    public void sendApprovalRequestToManager(String managerEmail, String managerName,
                                              String employeeName, String companyName,
                                              String eventType, String selectedGiftName,
                                              String managerToken) {
        if (!isConfigured()) return;

        String approveUrl = baseUrl + "/manager/" + managerToken + "/approve";
        String rejectUrl  = baseUrl + "/manager/" + managerToken + "/reject";

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(managerEmail);
            helper.setSubject("[GiftNova] Approval needed — " + employeeName + "'s " + eventType);
            helper.setText(
                "<div style='font-family:Arial,sans-serif;font-size:15px;color:#111;max-width:480px;margin:0 auto;padding:24px;'>" +
                "<p>Hi " + managerName + ",</p>" +
                "<p><strong>" + employeeName + "</strong> has selected their <strong>" + eventType + "</strong> gift:</p>" +
                "<p style='font-size:18px;font-weight:bold;margin:20px 0;'>\"" + selectedGiftName + "\"</p>" +
                "<p>Please take action:</p>" +
                "<div style='margin:28px 0;display:flex;gap:16px;'>" +
                "<a href='" + approveUrl + "' style='display:inline-block;padding:12px 28px;background:#16a34a;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;margin-right:12px;'>&#10003; Approve</a>" +
                "<a href='" + rejectUrl  + "' style='display:inline-block;padding:12px 28px;background:#dc2626;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;'>&#10005; Reject</a>" +
                "</div>" +
                "<p style='color:#666;font-size:13px;'>– GiftNova</p>" +
                "</div>",
                true
            );
            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send approval email: " + e.getMessage(), e);
        }
    }

    public String resolveManagerEmail(com.giftnova.model.Employee emp, com.giftnova.model.Company company) {
        if (emp != null && emp.getManagerEmail() != null && !emp.getManagerEmail().isBlank())
            return emp.getManagerEmail();
        return company.getAdminEmail();
    }

    public String resolveManagerName(com.giftnova.model.Employee emp, com.giftnova.model.Company company) {
        if (emp != null && emp.getManagerEmail() != null && !emp.getManagerEmail().isBlank()
                && emp.getManager() != null && !emp.getManager().isBlank())
            return emp.getManager();
        return company.getAdminName();
    }
}
