package com.pulsewatch.backend.alert.provider;

/**
 * Abstraction for any alert delivery channel.
 * Current implementation: EmailAlertProvider.
 * Future: SlackAlertProvider, WebhookAlertProvider, etc.
 */
public interface AlertProvider {
    void sendAlert(AlertContext context);
    boolean supports(AlertContext context);
}
