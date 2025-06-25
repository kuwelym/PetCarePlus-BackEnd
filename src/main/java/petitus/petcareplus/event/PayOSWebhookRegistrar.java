package petitus.petcareplus.event;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import petitus.petcareplus.configuration.PayOSConfig;
import vn.payos.PayOS;

@Component
public class PayOSWebhookRegistrar {

    private final PayOS payOS;

    private final PayOSConfig payOSConfig;

    public PayOSWebhookRegistrar(PayOS payOS, PayOSConfig payOSConfig) {
        this.payOSConfig = payOSConfig;
        this.payOS = payOS;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            String confirmedUrl = payOS.confirmWebhook(payOSConfig.getWebhookUrl());
            System.out.println("Webhook registered: " + confirmedUrl);
        } catch (Exception e) {
            System.err.println("Failed to register webhook: " + e.getMessage());
        }
    }
}
