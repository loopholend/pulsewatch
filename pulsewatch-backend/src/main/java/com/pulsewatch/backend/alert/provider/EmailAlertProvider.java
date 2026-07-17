package com.pulsewatch.backend.alert.provider;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EmailAlertProvider implements AlertProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertProvider.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@pulsewatch.io}")
    private String fromAddress;

    @Override
    public boolean supports(AlertContext context) {
        String recipient = context.getRecipient();
        return recipient != null && recipient.contains("@") && !recipient.startsWith("http");
    }

    @Override
    public void sendAlert(AlertContext context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(context.getRecipient());
            helper.setSubject(buildSubject(context));

            String htmlBody = buildHtmlBody(context);
            String plainTextBody = buildPlainTextBody(context);
            
            helper.setText(plainTextBody, htmlBody); // set both text and HTML versions

            mailSender.send(message);
            logger.info("HTML alert email sent to {} for monitor {} (type={})",
                    context.getRecipient(), context.getMonitorName(), context.getAlertType());
        } catch (Exception e) {
            logger.error("Failed to send MimeMessage HTML email: {}", e.getMessage(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String resolveTemplate(String template, AlertContext context) {
        if (template == null || template.isBlank()) return template;

        String status = "INCIDENT_OPENED".equals(context.getAlertType()) ? "DOWN" : "RESOLVED";
        String duration = context.getDurationSeconds() != null ? context.getDurationSeconds() + " seconds" : "N/A";
        String incidentId = context.getIncidentId() != null ? context.getIncidentId().toString() : "N/A";
        String timeStr = LocalDateTime.now().format(FORMATTER);

        return template
            .replace("{{monitor_name}}", context.getMonitorName() != null ? context.getMonitorName() : "")
            .replace("{{status}}", status)
            .replace("{{severity}}", context.getSeverity() != null ? context.getSeverity() : "INFO")
            .replace("{{timestamp}}", timeStr)
            .replace("{{incident_id}}", incidentId)
            .replace("{{duration}}", duration);
    }

    private String buildSubject(AlertContext context) {
        if (context.getCustomSubject() != null && !context.getCustomSubject().isBlank()) {
            return resolveTemplate(context.getCustomSubject(), context);
        }
        return switch (context.getAlertType()) {
            case "INCIDENT_OPENED" -> "[PulseWatch Alert] DOWN: " + context.getMonitorName();
            case "INCIDENT_RESOLVED" -> "[PulseWatch Alert] RECOVERED: " + context.getMonitorName();
            case "TEST" -> "[PulseWatch] Alert Configuration Test";
            default -> "[PulseWatch] Alert: " + context.getMonitorName();
        };
    }

    private String buildPlainTextBody(AlertContext context) {
        if (context.getCustomBody() != null && !context.getCustomBody().isBlank()) {
            return resolveTemplate(context.getCustomBody(), context);
        }
        return switch (context.getAlertType()) {
            case "INCIDENT_OPENED" -> String.format(
                    "PulseWatch Alert — Monitor Down\n" +
                    "Monitor: %s\n" +
                    "Time: %s\n" +
                    "Severity: %s\n" +
                    "An incident has been opened. Access your dashboard to view details.",
                    context.getMonitorName(), LocalDateTime.now().format(FORMATTER), context.getSeverity()
            );
            case "INCIDENT_RESOLVED" -> String.format(
                    "PulseWatch Alert — Monitor Recovered\n" +
                    "Monitor: %s\n" +
                    "Resolved: %s\n" +
                    "Downtime: %s seconds\n" +
                    "The incident has been resolved.",
                    context.getMonitorName(), LocalDateTime.now().format(FORMATTER), 
                    context.getDurationSeconds() != null ? context.getDurationSeconds() : "N/A"
            );
            case "TEST" -> "This is a test notification from PulseWatch. Your email alerting is working correctly.";
            default -> "PulseWatch Alert for monitor: " + context.getMonitorName();
        };
    }

    private String buildHtmlBody(AlertContext context) {
        boolean isDown = "INCIDENT_OPENED".equals(context.getAlertType());
        boolean isTest = "TEST".equals(context.getAlertType());

        String color = isDown ? "#ef4444" : (isTest ? "#3b82f6" : "#10b981");
        String titleColor = isDown ? "#991b1b" : (isTest ? "#1e3a8a" : "#065f46");
        String statusTitle = isDown ? "🔴 Monitor Down Alert" : (isTest ? "⚡ Test Alert Connection" : "🟢 Monitor Recovered");

        if (context.getCustomSubject() != null && !context.getCustomSubject().isBlank()) {
            statusTitle = resolveTemplate(context.getCustomSubject(), context);
        }

        String description = isDown 
                ? "An active outage has been detected. Service check assertions are failing."
                : (isTest ? "Your email alert notification endpoint has been configured successfully." : "Service check assertions are passing again. Incident resolved.");

        if (context.getCustomBody() != null && !context.getCustomBody().isBlank()) {
            description = resolveTemplate(context.getCustomBody(), context);
        }

        String layout = context.getLayoutType() != null ? context.getLayoutType().toUpperCase() : "DEFAULT";
        String timeStr = LocalDateTime.now().format(FORMATTER);

        if ("COMPACT".equals(layout)) {
            return String.format(
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <style>\n" +
                "    body { font-family: sans-serif; color: #374151; padding: 15px; margin: 0; background-color: #fafafa; }\n" +
                "    .wrapper { max-width: 500px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-left: 4px solid %s; padding: 20px; border-radius: 6px; }\n" +
                "    .title { font-size: 16px; font-weight: bold; color: %s; margin-top: 0; margin-bottom: 10px; }\n" +
                "    .desc { font-size: 14px; line-height: 1.4; color: #4b5563; margin-bottom: 15px; }\n" +
                "    .meta { font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6; padding-top: 10px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"wrapper\">\n" +
                "    <div class=\"title\">%s</div>\n" +
                "    <div class=\"desc\">%s</div>\n" +
                "    <div class=\"meta\">\n" +
                "      Monitor: <b>%s</b> | Severity: <b>%s</b><br/>\n" +
                "      Time: %s\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>",
                color, titleColor, statusTitle, description, context.getMonitorName(), context.getSeverity(), timeStr
            );
        } else if ("DETAILED".equals(layout)) {
            String durationRow = "";
            if (context.getDurationSeconds() != null && !isDown) {
                durationRow = String.format(
                    "<div style=\"display: table-row;\">" +
                    "  <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Downtime</div>" +
                    "  <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">%d seconds</div>" +
                    "</div>",
                    context.getDurationSeconds()
                );
            }

            String severityColor = getSeverityColorHtml(context.getSeverity());
            String runbookSection = isDown 
                ? "<div style=\"margin-top: 25px; padding: 15px; background-color: #fef2f2; border: 1px dashed #fca5a5; border-radius: 8px;\">" +
                  "  <h4 style=\"margin: 0 0 8px 0; color: #991b1b; font-size: 13px; font-weight: bold;\">🚨 Diagnostics & Troubleshooting</h4>" +
                  "  <ul style=\"margin: 0; padding-left: 20px; font-size: 12px; color: #7f1d1d; line-height: 1.6;\">" +
                  "    <li>Verify target API server status and network routing.</li>" +
                  "    <li>Check application logs for recent database failures or exception traces.</li>" +
                  "    <li>Review monitor headers and check assertions.</li>" +
                  "  </ul>" +
                  "</div>" 
                : "";

            String actionsSection = String.format(
                "<div style=\"margin-top: 20px; text-align: center;\">" +
                "  <a href=\"http://localhost:3000/incidents\" style=\"display: inline-block; background-color: #3b82f6; color: #ffffff; padding: 8px 16px; border-radius: 6px; font-size: 13px; font-weight: bold; text-decoration: none; margin-right: 10px;\">View Incident Details</a>" +
                "  <a href=\"http://localhost:3000/monitors/%s\" style=\"display: inline-block; border: 1px solid #d1d5db; color: #374151; padding: 8px 16px; border-radius: 6px; font-size: 13px; font-weight: bold; text-decoration: none;\">Monitor Config</a>" +
                "</div>",
                context.getMonitorId() != null ? context.getMonitorId().toString() : ""
            );

            return String.format(
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background-color: #f3f4f6; color: #1f2937; padding: 20px; margin: 0; }\n" +
                "    .card { background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; overflow: hidden; border-top: 6px solid %s; }\n" +
                "    .header { padding: 20px; background-color: #0f172a; color: #ffffff; text-align: center; font-size: 20px; font-weight: bold; letter-spacing: -0.025em; }\n" +
                "    .content { padding: 30px; }\n" +
                "    .status-title { font-size: 20px; font-weight: 800; margin-top: 0; color: %s; }\n" +
                "    .grid { display: table; width: 100%%; border-top: 1px solid #e5e7eb; border-bottom: 1px solid #e5e7eb; padding: 12px 0; margin: 20px 0; }\n" +
                "    .footer { text-align: center; padding: 20px; font-size: 12px; color: #9ca3af; }\n" +
                "    .footer a { color: #3b82f6; text-decoration: none; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <div class=\"header\">\n" +
                "      <span style=\"color:#3b82f6;\">⚡</span> PulseWatch Observability (Detailed)\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <h2 class=\"status-title\">%s</h2>\n" +
                "      <p style=\"font-size: 14px; color: #4b5563; line-height: 1.5; margin: 0;\">%s</p>\n" +
                "      \n" +
                "      <div class=\"grid\">\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Monitor</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">%s</div>\n" +
                "        </div>\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Incident ID</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0; font-family: monospace;\">%s</div>\n" +
                "        </div>\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Timestamp</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0; font-family: monospace;\">%s</div>\n" +
                "        </div>\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Severity</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">\n" +
                "            <span style=\"background-color: %s; color: #1f2937; padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: bold;\">%s</span>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        %s\n" +
                "      </div>\n" +
                "      \n" +
                "      %s\n" +
                "      %s\n" +
                "      \n" +
                "      <p style=\"font-size: 13px; color: #9ca3af; line-height: 1.5; margin-top: 25px; margin-bottom: 0;\">Log in to your PulseWatch console to review assertions configurations or active incident reports.</p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      Automated alerting system. Manage configurations at <a href=\"http://localhost:3000\">PulseWatch Console</a>.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>",
                color, titleColor, statusTitle, description, context.getMonitorName(), 
                context.getIncidentId() != null ? context.getIncidentId().toString() : "N/A", timeStr,
                severityColor, context.getSeverity(), durationRow, runbookSection, actionsSection
            );
        } else {
            // DEFAULT LAYOUT
            String durationRow = "";
            if (context.getDurationSeconds() != null && !isDown) {
                durationRow = String.format(
                    "<div style=\"display: table-row;\">" +
                    "  <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Downtime</div>" +
                    "  <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">%d seconds</div>" +
                    "</div>",
                    context.getDurationSeconds()
                );
            }

            String severityColor = getSeverityColorHtml(context.getSeverity());

            return String.format(
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background-color: #f3f4f6; color: #1f2937; padding: 20px; margin: 0; }\n" +
                "    .card { background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; overflow: hidden; border-top: 6px solid %s; }\n" +
                "    .header { padding: 20px; background-color: #0f172a; color: #ffffff; text-align: center; font-size: 20px; font-weight: bold; letter-spacing: -0.025em; }\n" +
                "    .content { padding: 30px; }\n" +
                "    .status-title { font-size: 20px; font-weight: 800; margin-top: 0; color: %s; }\n" +
                "    .grid { display: table; width: 100%%; border-top: 1px solid #e5e7eb; border-bottom: 1px solid #e5e7eb; padding: 12px 0; margin: 20px 0; }\n" +
                "    .footer { text-align: center; padding: 20px; font-size: 12px; color: #9ca3af; }\n" +
                "    .footer a { color: #3b82f6; text-decoration: none; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <div class=\"header\">\n" +
                "      <span style=\"color:#3b82f6;\">⚡</span> PulseWatch Observability\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <h2 class=\"status-title\">%s</h2>\n" +
                "      <p style=\"font-size: 14px; color: #4b5563; line-height: 1.5; margin: 0;\">%s</p>\n" +
                "      \n" +
                "      <div class=\"grid\">\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Monitor</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">%s</div>\n" +
                "        </div>\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Timestamp</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0; font-family: monospace;\">%s</div>\n" +
                "        </div>\n" +
                "        <div style=\"display: table-row;\">\n" +
                "          <div style=\"display: table-cell; font-size: 12px; font-weight: bold; color: #9ca3af; text-transform: uppercase; padding: 8px 0; width: 140px;\">Severity</div>\n" +
                "          <div style=\"display: table-cell; font-size: 14px; font-weight: 600; color: #374151; padding: 8px 0;\">\n" +
                "            <span style=\"background-color: %s; color: #1f2937; padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: bold;\">%s</span>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        %s\n" +
                "      </div>\n" +
                "      \n" +
                "      <p style=\"font-size: 13px; color: #9ca3af; line-height: 1.5; margin: 0;\">Log in to your PulseWatch console to review assertions configurations or active incident reports.</p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      Automated alerting system. Manage configurations at <a href=\"http://localhost:3000\">PulseWatch Console</a>.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>",
                color, titleColor, statusTitle, description, context.getMonitorName(), timeStr,
                severityColor, context.getSeverity(), durationRow
            );
        }
    }

    private String getSeverityColorHtml(String severity) {
        if (severity == null) return "#e5e7eb";
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "#fecaca"; // red
            case "HIGH" -> "#fed7aa"; // orange
            case "MEDIUM" -> "#fde68a"; // yellow
            default -> "#f3f4f6"; // gray
        };
    }
}
