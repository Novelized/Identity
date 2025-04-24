package online.novelized.id;

import online.novelized.id.config.DefaultClientProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

@SpringBootApplication
@EnableConfigurationProperties(DefaultClientProperties.class)
public class IdApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(
            RegisteredClientRepository repository,
            PasswordEncoder passwordEncoder,
            DefaultClientProperties clientProperties) {
        return args -> {
            String clientId = clientProperties.getClientId();
            if (repository.findByClientId(clientId) == null) {
                RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(clientId)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);

                clientProperties.getRedirectUris().forEach(builder::redirectUri);

                clientProperties.getScopes().forEach(builder::scope);

                builder.clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(clientProperties.isRequireConsent())
                        .build());

                repository.save(builder.build());
                System.out.println("###### Default OAuth2 client created: " + clientId);
            }
        };
    }

}
