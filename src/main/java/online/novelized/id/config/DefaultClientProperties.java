package online.novelized.id.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

@ConfigurationProperties(prefix = "app.security.oauth2.default-client")
public class DefaultClientProperties {

    private String clientId = "default-client";
    private String clientSecret = "secret";
    private Set<String> redirectUris = Collections.emptySet();
    private Set<String> scopes = Set.of("openid", "profile", "message.read", "message.write");
    private boolean requireConsent = true;

    // Getters and Setters

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public boolean isRequireConsent() {
        return requireConsent;
    }

    public void setRequireConsent(boolean requireConsent) {
        this.requireConsent = requireConsent;
    }
} 