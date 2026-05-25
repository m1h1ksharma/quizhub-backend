package com.piet.quizhub.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends a professional HTML email with the password reset link.
     * @param to Student's email address
     * @param token Unique UUID generated in AuthController
     */
    public void sendResetLink(String to, String token) {
        // Frontend URL jahan bacha naya password enter karega
        // EmailService.java mein jahan link banta hai
// ✅ userEmail ko hata kar 'to' likh do kyunki parameter ka naam 'to' hai
String resetLink = "http://localhost:3000/reset-password?token=" + token + "&email=" + to;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Reset Your QuizHub Password");

            // Professional HTML Template
            String htmlContent = 
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px;'>" +
                "  <h2 style='color: #2563eb; text-align: center;'>QuizHub Recovery</h2>" +
                "  <p>Hello,</p>" +
                "  <p>We have received a request to reset your QuizHub password.</p>" +
                "  <div style='text-align: center; margin: 30px 0;'>" +
                "    <a href='" + resetLink + "' style='background-color: #2563eb; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Reset Password</a>" +
                "  </div>" +
                "  <p style='color: #64748b; font-size: 12px;'>This link is valid for 15 minutes. If you did not request this, please ignore this email.</p>" +
                "  <hr style='border: 0; border-top: 1px solid #f1f5f9;'>" +
                "  <p style='text-align: center; color: #94a3b8; font-size: 11px;'>PIET QuizHub Assessment Portal - Batch 2026</p>" +
                "</div>";

            helper.setText(htmlContent, true); 
            mailSender.send(message);
            
            System.out.println("Email successfully sent to: " + to);

        } catch (MessagingException e) {
            System.err.println("Error while sending email: " + e.getMessage());
            throw new RuntimeException("Email service down hai, please try again later.");
        }
    }
}