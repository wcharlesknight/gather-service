package com.gather.service;

import com.gather.model.domain.Place;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:noreply@gather.app}")
    private String fromEmail;

    @Value("${resend.enabled:true}")
    private boolean enabled;

    // Created once and reused, rather than per send, so the underlying HTTP client can pool.
    private Resend resend;

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            this.resend = new Resend(apiKey);
        }
    }

    public void sendGatheringSpotEmail(Place place, String toEmail, String displayName) {
        if (!enabled) {
            logger.warn("Resend is disabled. Skipping email to {}", maskEmail(toEmail));
            return;
        }
        if (resend == null) {
            logger.warn("RESEND_API_KEY not set. Skipping email to {}", maskEmail(toEmail));
            return;
        }

        try {
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject("📍 This Week's Gather Spot!")
                    .html(buildEmailBody(place, displayName))
                    .build();

            resend.emails().send(email);
            logger.info("Email sent to {}", maskEmail(toEmail));
        } catch (ResendException e) {
            logger.error("Failed to send email to {}: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    private String buildEmailBody(Place place, String displayName) {
        // All interpolated values are HTML-escaped: displayName is user-controlled and place
        // fields come from an external API, so neither can be trusted as raw HTML.
        String name = HtmlUtils.htmlEscape(displayName != null ? displayName : "there");
        String placeName = HtmlUtils.htmlEscape(place.getName() != null ? place.getName() : "");
        String address = HtmlUtils.htmlEscape(place.getAddress() != null ? place.getAddress() : "");
        String rating = place.getRating() != null ? String.format("%.1f ⭐", place.getRating()) : "N/A";
        String mapsLink = isHttpsUrl(place.getUrl())
                ? "<a href=\"" + HtmlUtils.htmlEscape(place.getUrl()) + "\">View on Google Maps</a>"
                : "";

        return "<div style='font-family: sans-serif; max-width: 480px; margin: auto;'>"
                + "<h2>Hey " + name + ", this Friday's gather spot is ready! 🎉</h2>"
                + "<h3>" + placeName + "</h3>"
                + "<p>" + address + "</p>"
                + "<p>Rating: " + rating + "</p>"
                + "<p>" + mapsLink + "</p>"
                + "<p style='color: #888; font-size: 12px;'>See you Friday!</p>"
                + "</div>";
    }

    private static boolean isHttpsUrl(String url) {
        return url != null && url.startsWith("https://");
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<none>";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
