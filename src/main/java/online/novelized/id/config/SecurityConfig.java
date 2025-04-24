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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
     * Configures the default security filter chain for handling user authentication (e.g., login form)
     * and API endpoints secured with OAuth2 tokens.
     *
     * @param http HttpSecurity configuration object.
     * @return Configured SecurityFilterChain.
     * @throws Exception If configuration fails.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS globally for this chain
            .cors(withDefaults())
            // Stateless session management suitable for APIs
            // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Explicitly permit OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/messages").permitAll()
                // Allow unauthenticated access to static resources, login page, etc.
                // .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/error").permitAll()
                // Secure API endpoints like /messages - Require JWT with 'message.read' scope
                .requestMatchers("/messages").hasAuthority("SCOPE_message.read") // Use hasAuthority for scope check
                // Secure all other requests with standard form login
                .anyRequest().authenticated()
            )
            // Configure OAuth2 Resource Server support for validating JWTs
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
            // Standard Form login for browser-based interaction
            .formLogin(withDefaults());


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
     * Provides the source for JWT signing keys. Generates an RSA key pair on startup.
     * For production, keys should be loaded securely, not generated.
     *
     * @return JWKSource containing the signing key.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

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
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

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
     * Defines the CORS configuration source.
     *
     * @return CorsConfigurationSource bean.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow requests from the frontend development server
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
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