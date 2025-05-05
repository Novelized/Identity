package online.novelized.id.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Arrays;
import java.util.UUID;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.http.SessionCreationPolicy;
import online.novelized.id.config.CorsProperties;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    // Inject the Base64 encoded PEM private key from environment variable
    @Value("${JWT_PRIVATE_KEY_BASE64_PEM:#{null}}") // Default to null if not set
    private String jwtPrivateKeyBase64Pem;

    /**
     * Configures the security filter chain for the Authorization Server endpoints.
     *
     * @param http HttpSecurity configuration object.
     * @return Configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Enable OpenID Connect 1.0 features
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());

        http
            // Redirect to the login page when not authenticated from the authorization endpoint
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            )
            // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults()));

        // Enable CORS
        http.cors(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Configures the security filter chain for stateless API endpoints secured with OAuth2 tokens.
     *
     * @param http HttpSecurity configuration object.
     * @return Configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Match only API paths (adjust pattern as needed)
            .securityMatcher("/messages/**", "/api/**") // Changed /messages to /messages/**
            // Disable CSRF protection for stateless API requests authenticated via Bearer tokens
            .csrf(AbstractHttpConfigurer::disable)
            // Enable CORS globally for this chain
            .cors(withDefaults())
            // Enforce stateless session management for APIs
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Explicitly permit OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/messages", "/api/**").permitAll()
                // Secure API endpoints like /messages - Require JWT with 'message.read' scope
                .requestMatchers("/messages").hasAuthority("SCOPE_message.read") // Use hasAuthority for scope check
                .anyRequest().authenticated() // Secure all matched API requests
            )
            // Configure OAuth2 Resource Server support for validating JWTs
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    /**
     * Configures the default security filter chain for handling user authentication (e.g., login form)
     * and other browser-based interactions. CSRF protection is ENABLED for this chain.
     *
     * @param http HttpSecurity configuration object.
     * @return Configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(3) // Lower precedence than API chain
    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
             // Enable CORS (consider if needed for form login/UI pages)
            .cors(withDefaults())
            // CSRF is enabled by default, protecting form login
            .authorizeHttpRequests(authorize -> authorize
                 // Allow unauthenticated access to static resources, login page, etc.
                 // .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/error").permitAll()
                // Secure all other requests (including those falling through from the API chain)
                .anyRequest().authenticated()
            )
            // Standard Form login for browser-based interaction
            .formLogin(withDefaults());
            // Session management is stateful by default (good for form login)

        return http.build();
    }

    /**
     * Provides the repository for managing registered OAuth2 clients using JDBC.
     *
     * @param jdbcTemplate      The JdbcTemplate for database interaction.
     * @return RegisteredClientRepository implementation.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        // Uses the V1__Init_Auth_Server_Schema.sql script
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * Provides the service for managing OAuth2 authorizations using JDBC.
     *
     * @param jdbcTemplate              The JdbcTemplate for database interaction.
     * @param registeredClientRepository The repository for client details.
     * @return OAuth2AuthorizationService implementation.
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        // Uses the V1__Init_Auth_Server_Schema.sql script
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Provides the service for managing OAuth2 authorization consents using JDBC.
     *
     * @param jdbcTemplate              The JdbcTemplate for database interaction.
     * @param registeredClientRepository The repository for client details.
     * @return OAuth2AuthorizationConsentService implementation.
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        // This expects the oauth2_authorization_consent table
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Provides the source for JWT signing keys (JWKSource).
     *
     * Attempts to load an RSA private key from the 'JWT_PRIVATE_KEY_BASE64_PEM' environment variable.
     * The variable should contain the Base64 encoded, full PEM representation of the PKCS#8 private key
     * (including the '-----BEGIN PRIVATE KEY-----' and '-----END PRIVATE KEY-----' markers).
     *
     * If the environment variable is NOT SET or empty, a temporary RSA key pair will be
     * generated IN MEMORY for development convenience. THIS IS NOT SUITABLE FOR PRODUCTION.
     *
     * IMPORTANT: For production, ensure the JWT_PRIVATE_KEY_BASE64_PEM environment variable is securely set.
     *
     * @return JWKSource containing the signing key.
     * @throws IllegalStateException if the environment variable is set but the key is invalid.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        if (StringUtils.hasText(jwtPrivateKeyBase64Pem)) {
            log.info("Attempting to load JWT signing key from JWT_PRIVATE_KEY_BASE64_PEM environment variable.");
            try {
                RSAPrivateKey privateKey = parsePemPrivateKey(jwtPrivateKeyBase64Pem);
                RSAPublicKey publicKey = derivePublicKey(privateKey);

                RSAKey rsaKey = new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID(UUID.randomUUID().toString())
                        .build();

                JWKSet jwkSet = new JWKSet(rsaKey);
                log.info("Successfully loaded JWT signing key from environment variable.");
                return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);

            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                log.error("Failed to parse RSA private key from environment variable JWT_PRIVATE_KEY_BASE64_PEM. Check format.", e);
                throw new IllegalStateException("Failed to parse RSA private key from environment variable JWT_PRIVATE_KEY_BASE64_PEM", e);
            } catch (IllegalArgumentException e) {
                log.error("Invalid Base64 encoding for JWT_PRIVATE_KEY_BASE64_PEM environment variable.", e);
                 throw new IllegalStateException("Invalid Base64 encoding for JWT_PRIVATE_KEY_BASE64_PEM", e);
            } catch (Exception e) {
                log.error("An unexpected error occurred while loading JWKSource from environment variable.", e);
                throw new IllegalStateException("An unexpected error occurred while configuring JWKSource from environment variable", e);
            }
        } else {
            // --- DEVELOPMENT ONLY FALLBACK: Generate RSA key in memory ---
            log.warn("****** WARNING ******");
            log.warn("JWT_PRIVATE_KEY_BASE64_PEM environment variable not set.");
            log.warn("Generating temporary RSA signing keys for JWTs. DO NOT USE IN PRODUCTION.");
            log.warn("Keys will be lost on application restart, invalidating existing tokens.");
            log.warn("Set the JWT_PRIVATE_KEY_BASE64_PEM environment variable for persistent keys.");
            log.warn("*********************");

            RSAKey rsaKey = generateRsa();
            JWKSet jwkSet = new JWKSet(rsaKey);
            return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
            // --- END DEVELOPMENT ONLY FALLBACK ---
        }
    }

    /**
     * Parses a Base64 encoded PEM string (PKCS#8 format, including headers/footers) into an RSAPrivateKey.
     *
     * @param base64FullPem The Base64 encoded string of the full PEM file content.
     * @return The RSAPrivateKey instance.
     * @throws NoSuchAlgorithmException If RSA algorithm is not available.
     * @throws InvalidKeySpecException If the key spec is invalid.
     * @throws IllegalArgumentException If Base64 decoding fails or PEM format is incorrect.
     */
    private static RSAPrivateKey parsePemPrivateKey(String base64FullPem) throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {
        // 1. Decode the outer Base64 to get the full PEM string
        byte[] decodedPemBytes = Base64.getDecoder().decode(base64FullPem);
        String pemContent = new String(decodedPemBytes).trim();

        // 2. Remove PEM headers/footers
        String privateKeyContent = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace (newlines, spaces)

        if (privateKeyContent.isEmpty()) {
             throw new IllegalArgumentException("PEM content is empty after removing headers/footers.");
        }

        // 3. Decode the inner Base64 content to get the PKCS#8 bytes
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKeyContent);

        // 4. Use the decoded bytes to create the key spec
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(keySpec);

        if (!(key instanceof RSAPrivateKey)) {
            throw new InvalidKeySpecException("Key provided is not an RSA private key");
        }
        return (RSAPrivateKey) key;
    }

    /**
     * Derives the RSAPublicKey from an RSAPrivateCrtKey.
     *
     * @param privateKey The RSAPrivateKey (must be RSAPrivateCrtKey).
     * @return The corresponding RSAPublicKey.
     * @throws NoSuchAlgorithmException If RSA algorithm is not available.
     * @throws InvalidKeySpecException If the key spec cannot be derived.
     */
    private static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (privateKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
             if (!(publicKey instanceof RSAPublicKey)) {
                throw new InvalidKeySpecException("Derived key is not an RSA public key");
             }
            return (RSAPublicKey) publicKey;
        } else {
            throw new InvalidKeySpecException("Cannot derive public key from a non-CRT private key");
        }
    }

    // --- Helper methods for DEVELOPMENT ONLY key generation ---
    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            // Log the exception for better debugging
            log.error("Failed to generate RSA key pair for development fallback.", ex);
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
        return keyPair;
    }
    // --- END Helper methods ---

    /**
     * Provides the settings for the authorization server
     *
     * @return AuthorizationServerSettings.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // Explicitly set the issuer URI. will be altered in the future
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }

    /**
     * Provides the password encoder bean.
     *
     * @return PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the CORS configuration source, using allowed origins from properties.
     *
     * @return CorsConfigurationSource bean.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowed origins from configuration properties
        log.info("Configuring CORS with allowed origins: {}", corsProperties.getAllowedOrigins());
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // Allow credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this configuration to all paths
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // TODO: Replace with a real UserDetailsService in production
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                // Store the password encoded
                .password(passwordEncoder().encode("password")) 
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}