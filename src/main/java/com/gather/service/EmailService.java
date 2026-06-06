package com.gather.service;

import com.gather.model.domain.Place;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:noreply@gather.app}")
    private String fromEmail;

    @Value("${resend.enabled:true}")
    private boolean enabled;

    public void sendGatheringSpotEmail(Place place, String toEmail, String displayName) {
        if (!enabled) {
            logger.warn("Resend is disabled. Skipping email to {}", toEmail);
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("RESEND_API_KEY not set. Skipping email to {}", toEmail);
            return;
        }

        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject("📍 This Week's Gather Spot!")
                    .html(buildEmailBody(place, displayName))
                    .build();

            resend.emails().send(email);
            logger.info("Email sent to {}", toEmail);
        } catch (ResendException e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(Place place, String displayName) {
        String name = displayName != null ? displayName : "there";
        String rating = place.getRating() != null ? String.format("%.1f ⭐", place.getRating()) : "N/A";
        String mapsLink = place.getUrl() != null
                ? "<a href=\"" + place.getUrl() + "\">View on Google Maps</a>"
                : "";

        return "<div style='font-family: sans-serif; max-width: 480px; margin: auto;'>"
                + "<h2>Hey " + name + ", this Friday's gather spot is ready! 🎉</h2>"
                + "<h3>" + place.getName() + "</h3>"
                + "<p>" + place.getAddress() + "</p>"
                + "<p>Rating: " + rating + "</p>"
                + "<p>" + mapsLink + "</p>"
                + "<p style='color: #888; font-size: 12px;'>See you Friday!</p>"
                + "</div>";
    }
}
