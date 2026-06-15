package com.expensewise.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ───────────────────────────────────────────────
    // Public methods
    // ───────────────────────────────────────────────

    public void sendWelcomeEmail(String fullName, String email) {
        try {
            String bodyContent =
                    "<h2 style=\"color: #1f2937; margin-top: 0;\">Welcome, " + fullName + "! 🎉</h2>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "We're thrilled to have you on board. ExpenseWise is your personal finance companion "
                    + "designed to help you take control of your money and achieve your financial goals."
                    + "</p>"
                    + "<div style=\"margin: 30px 0;\">"
                    + "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse;\">"
                    + "    <tr>"
                    + "      <td style=\"padding: 15px; background-color: #f0fdf4; border-left: 4px solid #4ade80; margin-bottom: 10px; border-radius: 0 8px 8px 0;\">"
                    + "        <strong style=\"color: #166534;\">📊 Track Expenses</strong>"
                    + "        <p style=\"margin: 5px 0 0; color: #374151; font-size: 14px;\">Log and categorize every transaction effortlessly.</p>"
                    + "      </td>"
                    + "    </tr>"
                    + "    <tr><td style=\"height: 10px;\"></td></tr>"
                    + "    <tr>"
                    + "      <td style=\"padding: 15px; background-color: #f0fdf4; border-left: 4px solid #4ade80; margin-bottom: 10px; border-radius: 0 8px 8px 0;\">"
                    + "        <strong style=\"color: #166534;\">💰 Set Budgets</strong>"
                    + "        <p style=\"margin: 5px 0 0; color: #374151; font-size: 14px;\">Create monthly budgets and stay on track with alerts.</p>"
                    + "      </td>"
                    + "    </tr>"
                    + "    <tr><td style=\"height: 10px;\"></td></tr>"
                    + "    <tr>"
                    + "      <td style=\"padding: 15px; background-color: #f0fdf4; border-left: 4px solid #4ade80; margin-bottom: 10px; border-radius: 0 8px 8px 0;\">"
                    + "        <strong style=\"color: #166534;\">🎯 Savings Goals</strong>"
                    + "        <p style=\"margin: 5px 0 0; color: #374151; font-size: 14px;\">Set goals, track progress, and celebrate milestones.</p>"
                    + "      </td>"
                    + "    </tr>"
                    + "  </table>"
                    + "</div>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "Start by adding your first expense or setting up a budget. We're here to help you every step of the way!"
                    + "</p>";

            String html = buildHtmlEmail("Welcome to ExpenseWise!", bodyContent);
            sendHtmlMail(email, "Welcome to ExpenseWise! 🎉", html);
            log.info("Welcome email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage(), e);
        }
    }

    public void sendGoalReachedEmail(String fullName, String email, String goalName, BigDecimal targetAmount) {
        try {
            String formattedAmount = formatCurrency(targetAmount);

            String bodyContent =
                    "<h2 style=\"color: #1f2937; margin-top: 0;\">Congratulations, " + fullName + "! 🎯🎉</h2>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "You've done something amazing — you've reached your savings goal!"
                    + "</p>"
                    + "<div style=\"margin: 25px 0; padding: 25px; background-color: #f0fdf4; border-radius: 12px; border: 1px solid #bbf7d0; text-align: center;\">"
                    + "  <p style=\"margin: 0 0 5px; color: #6b7280; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Goal Completed</p>"
                    + "  <h3 style=\"margin: 0 0 10px; color: #166534; font-size: 22px;\">" + goalName + "</h3>"
                    + "  <p style=\"margin: 0; color: #15803d; font-size: 32px; font-weight: 700;\">" + formattedAmount + "</p>"
                    + "</div>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "Your dedication and discipline have paid off. Saving " + formattedAmount
                    + " is no small feat, and you should be proud of this achievement."
                    + "</p>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "Ready for the next challenge? Head over to ExpenseWise and set a new savings goal to keep the momentum going! 🚀"
                    + "</p>";

            String html = buildHtmlEmail("Goal Achieved!", bodyContent);
            sendHtmlMail(email, "🎯 Goal Achieved! You reached your savings target!", html);
            log.info("Goal reached email sent successfully to {} for goal '{}'", email, goalName);
        } catch (Exception e) {
            log.error("Failed to send goal reached email to {}: {}", email, e.getMessage(), e);
        }
    }

    public void sendBudgetExceededEmail(String fullName, String email, String categoryName,
                                        BigDecimal budgetLimit, BigDecimal spent) {
        try {
            String formattedLimit = formatCurrency(budgetLimit);
            String formattedSpent = formatCurrency(spent);

            String bodyContent =
                    "<h2 style=\"color: #1f2937; margin-top: 0;\">Budget Alert ⚠️</h2>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "Hi " + fullName + ", this is a friendly heads-up that you've exceeded your budget."
                    + "</p>"
                    + "<div style=\"margin: 25px 0; padding: 25px; background-color: #fef2f2; border-radius: 12px; border: 1px solid #fecaca;\">"
                    + "  <p style=\"margin: 0 0 15px; color: #991b1b; font-size: 18px; font-weight: 600; text-align: center;\">"
                    + "    " + categoryName
                    + "  </p>"
                    + "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse;\">"
                    + "    <tr>"
                    + "      <td style=\"padding: 10px 15px; text-align: center; width: 50%;\">"
                    + "        <p style=\"margin: 0 0 5px; color: #6b7280; font-size: 13px; text-transform: uppercase; letter-spacing: 1px;\">Budget Limit</p>"
                    + "        <p style=\"margin: 0; color: #166534; font-size: 24px; font-weight: 700;\">" + formattedLimit + "</p>"
                    + "      </td>"
                    + "      <td style=\"padding: 10px 15px; text-align: center; width: 50%;\">"
                    + "        <p style=\"margin: 0 0 5px; color: #6b7280; font-size: 13px; text-transform: uppercase; letter-spacing: 1px;\">Actual Spent</p>"
                    + "        <p style=\"margin: 0; color: #dc2626; font-size: 24px; font-weight: 700;\">" + formattedSpent + "</p>"
                    + "      </td>"
                    + "    </tr>"
                    + "  </table>"
                    + "</div>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "We recommend reviewing your recent transactions in the <strong>" + categoryName
                    + "</strong> category. Small adjustments now can help you stay on track for the rest of the month."
                    + "</p>"
                    + "<p style=\"color: #374151; font-size: 16px; line-height: 1.6;\">"
                    + "Consider revising your budget or identifying non-essential expenses you can cut back on. You've got this! 💪"
                    + "</p>";

            String html = buildHtmlEmail("Budget Exceeded", bodyContent);
            sendHtmlMail(email, "⚠️ Budget Alert: You've exceeded your budget!", html);
            log.info("Budget exceeded email sent successfully to {} for category '{}'", email, categoryName);
        } catch (Exception e) {
            log.error("Failed to send budget exceeded email to {}: {}", email, e.getMessage(), e);
        }
    }

    // ───────────────────────────────────────────────
    // Private helpers
    // ───────────────────────────────────────────────

    private String buildHtmlEmail(String title, String bodyContent) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "  <meta charset=\"UTF-8\">"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "  <title>" + title + "</title>"
                + "</head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f3f4f6; font-family: 'Segoe UI', Arial, sans-serif;\">"
                + "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f3f4f6;\">"
                + "    <tr>"
                + "      <td align=\"center\" style=\"padding: 30px 15px;\">"
                + "        <table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; width: 100%; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.07);\">"
                // Header
                + "          <tr>"
                + "            <td style=\"background-color: #111827; padding: 30px 40px; text-align: center;\">"
                + "              <h1 style=\"margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px;\">"
                + "                <span style=\"color: #4ade80;\">Expense</span><span style=\"color: #ffffff;\">Wise</span>"
                + "              </h1>"
                + "            </td>"
                + "          </tr>"
                // Content
                + "          <tr>"
                + "            <td style=\"background-color: #ffffff; padding: 40px;\">"
                + "              " + bodyContent
                + "            </td>"
                + "          </tr>"
                // Footer
                + "          <tr>"
                + "            <td style=\"background-color: #f9fafb; padding: 25px 40px; text-align: center; border-top: 1px solid #e5e7eb;\">"
                + "              <p style=\"margin: 0 0 5px; color: #6b7280; font-size: 14px;\">The ExpenseWise Team</p>"
                + "              <p style=\"margin: 0; color: #9ca3af; font-size: 12px;\">This is an automated message. Please do not reply directly to this email.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private void sendHtmlMail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(amount);
    }
}
