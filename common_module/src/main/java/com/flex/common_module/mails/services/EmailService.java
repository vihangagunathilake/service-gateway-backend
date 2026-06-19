package com.flex.common_module.mails.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.frontend.url}")
    private String baseUrl;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public void newPassword(String to, String token, String name) throws MessagingException {

        String link = baseUrl + "/login";

        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("name", name);
        context.setVariable("link", link);

        String htmlContent =
                templateEngine.process("register-token", context);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("System access token for Registration");
        helper.setText(htmlContent, true);

        mailSender.send(message);

        log.info("send email to {}", to);
    }

    public void sendPasswordResetEmail(String to, String newToken, String name) throws MessagingException {

        Context context = new Context();
        context.setVariable("token", newToken);
        context.setVariable("name", name);

        String htmlContent =
                templateEngine.process("password-reset", context);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Password Reset Token");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
