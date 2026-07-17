package com.pulsewatch.backend.alert.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SlackAlertProvider implements AlertProvider {

    private static final Logger logger = LoggerFactory.getLogger(SlackAlertProvider.class);
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean supports(AlertContext context) {
        String recipient = context.getRecipient();
        return recipient != null && 
                (recipient.startsWith("http://") || recipient.startsWith("https://")) && 
                recipient.contains("slack.com");
    }

    @Override
    public void sendAlert(AlertContext context) {
        try {
            String jsonPayload = buildSlackBlockPayload(context);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(context.getRecipient()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                logger.error("Failed to send Slack Block Kit alert. Status code: {}, Response: {}", 
                        response.statusCode(), response.body());
            } else {
                logger.info("Slack Block Kit alert sent successfully to {}", context.getRecipient());
            }
        } catch (Exception e) {
            logger.error("Error sending Slack Block Kit alert to {}: {}", context.getRecipient(), e.getMessage(), e);
            throw new RuntimeException("Slack alert dispatch failed", e);
        }
    }

    private static final java.time.format.DateTimeFormatter FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String resolveTemplate(String template, AlertContext context) {
        if (template == null || template.isBlank()) return template;

        String status = "INCIDENT_OPENED".equals(context.getAlertType()) ? "DOWN" : "RESOLVED";
        String duration = context.getDurationSeconds() != null ? context.getDurationSeconds() + " seconds" : "N/A";
        String incidentId = context.getIncidentId() != null ? context.getIncidentId().toString() : "N/A";
        String timeStr = java.time.LocalDateTime.now().format(FORMATTER);

        return template
            .replace("{{monitor_name}}", context.getMonitorName() != null ? context.getMonitorName() : "")
            .replace("{{status}}", status)
            .replace("{{severity}}", context.getSeverity() != null ? context.getSeverity() : "INFO")
            .replace("{{timestamp}}", timeStr)
            .replace("{{incident_id}}", incidentId)
            .replace("{{duration}}", duration);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String buildSlackBlockPayload(AlertContext context) {
        boolean isDown = "INCIDENT_OPENED".equals(context.getAlertType());
        boolean isTest = "TEST".equals(context.getAlertType());

        String color = isDown ? "#ef4444" : (isTest ? "#3b82f6" : "#10b981");
        String headerText = isDown ? "🔴 Outage Detected" : (isTest ? "⚡ PulseWatch Alert Channel Test" : "🟢 Service Restored");

        if (context.getCustomSubject() != null && !context.getCustomSubject().isBlank()) {
            headerText = resolveTemplate(context.getCustomSubject(), context);
        }

        String messageText = isDown 
                ? "*Outage Active:* Telemetry check assertions are failing." 
                : (isTest ? "Your Slack integration webhook is configured correctly." : "*Outage Resolved:* All telemetry check assertions are passing.");

        if (context.getCustomBody() != null && !context.getCustomBody().isBlank()) {
            messageText = resolveTemplate(context.getCustomBody(), context);
        }

        headerText = escapeJson(headerText);
        messageText = escapeJson(messageText);

        String layout = context.getLayoutType() != null ? context.getLayoutType().toUpperCase() : "DEFAULT";

        if ("COMPACT".equals(layout)) {
            return String.format(
                "{" +
                "  \"attachments\": [" +
                "    {" +
                "      \"color\": \"%s\"," +
                "      \"blocks\": [" +
                "        {" +
                "          \"type\": \"section\"," +
                "          \"text\": {" +
                "            \"type\": \"mrkdwn\"," +
                "            \"text\": \"*%s*\\n%s\"" +
                "          }" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}",
                color, headerText, messageText
            );
        } else if ("DETAILED".equals(layout)) {
            String durationField = "";
            if (context.getDurationSeconds() != null && !isDown) {
                durationField = String.format(", { \"type\": \"mrkdwn\", \"text\": \"*Downtime Duration:*\\n%d seconds\" }", context.getDurationSeconds());
            }

            String incidentId = context.getIncidentId() != null ? context.getIncidentId().toString() : "N/A";
            String monitorId = context.getMonitorId() != null ? context.getMonitorId().toString() : "";
            String timeStr = java.time.LocalDateTime.now().format(FORMATTER);

            return String.format(
                "{" +
                "  \"attachments\": [" +
                "    {" +
                "      \"color\": \"%s\"," +
                "      \"blocks\": [" +
                "        {" +
                "          \"type\": \"header\"," +
                "          \"text\": {" +
                "            \"type\": \"plain_text\"," +
                "            \"text\": \"%s\"," +
                "            \"emoji\": true" +
                "          }" +
                "        }," +
                "        {" +
                "          \"type\": \"section\"," +
                "          \"text\": {" +
                "            \"type\": \"mrkdwn\"," +
                "            \"text\": \"%s\"" +
                "          }" +
                "        }," +
                "        {" +
                "          \"type\": \"section\"," +
                "          \"fields\": [" +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Monitor:*\\n%s\" }," +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Severity:*\\n%s\" }," +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Incident ID:*\\n%s\" }," +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Detected At:*\\n%s\" }" +
                "            %s" +
                "          ]" +
                "        }," +
                "        {" +
                "          \"type\": \"actions\"," +
                "          \"elements\": [" +
                "            {" +
                "              \"type\": \"button\"," +
                "              \"text\": {" +
                "                \"type\": \"plain_text\"," +
                "                \"text\": \"View Dashboard ↗\"" +
                "              }," +
                "              \"url\": \"http://localhost:3000/incidents\"," +
                "              \"style\": \"primary\"" +
                "            }," +
                "            {" +
                "              \"type\": \"button\"," +
                "              \"text\": {" +
                "                \"type\": \"plain_text\"," +
                "                \"text\": \"Configure Monitor\"" +
                "              }," +
                "              \"url\": \"http://localhost:3000/monitors/%s\"" +
                "            }" +
                "          ]" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}",
                color, headerText, messageText, escapeJson(context.getMonitorName()), 
                escapeJson(context.getSeverity()), incidentId, timeStr, durationField, monitorId
            );
        } else {
            String durationField = "";
            if (context.getDurationSeconds() != null && !isDown) {
                durationField = String.format(", { \"type\": \"mrkdwn\", \"text\": \"*Downtime Duration:*\\n%d seconds\" }", context.getDurationSeconds());
            }

            return String.format(
                "{" +
                "  \"attachments\": [" +
                "    {" +
                "      \"color\": \"%s\"," +
                "      \"blocks\": [" +
                "        {" +
                "          \"type\": \"header\"," +
                "          \"text\": {" +
                "            \"type\": \"plain_text\"," +
                "            \"text\": \"%s\"," +
                "            \"emoji\": true" +
                "          }" +
                "        }," +
                "        {" +
                "          \"type\": \"section\"," +
                "          \"text\": {" +
                "            \"type\": \"mrkdwn\"," +
                "            \"text\": \"%s\"" +
                "          }" +
                "        }," +
                "        {" +
                "          \"type\": \"section\"," +
                "          \"fields\": [" +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Monitor:*\\n%s\" }," +
                "            { \"type\": \"mrkdwn\", \"text\": \"*Severity:*\\n%s\" }" +
                "            %s" +
                "          ]" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}",
                color, headerText, messageText, escapeJson(context.getMonitorName()), escapeJson(context.getSeverity()), durationField
            );
        }
    }
}
