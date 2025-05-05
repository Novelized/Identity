package online.novelized.id.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 */
@ConfigurationProperties(prefix = "app.cors")
@Validated
public class CorsProperties {

    /**
     * List of allowed origins for CORS requests.
     * Example: ["http://localhost:5173"]
     */
    @NotEmpty // Ensure at least one origin is configured
    private List<String> allowedOrigins;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
} 