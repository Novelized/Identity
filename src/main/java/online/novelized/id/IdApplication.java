package online.novelized.id;

import online.novelized.id.config.DefaultClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

@SpringBootApplication
@EnableConfigurationProperties(DefaultClientProperties.class)
public class IdApplication {

    private static final Logger log = LoggerFactory.getLogger(IdApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(
            RegisteredClientRepository repository,
            DefaultClientProperties clientProperties) {
        return args -> {
            String clientId = clientProperties.getClientId();
            if (repository.findByClientId(clientId) == null) {
                RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(clientId)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

                clientProperties.getRedirectUris().forEach(builder::redirectUri);

                clientProperties.getScopes().forEach(builder::scope);

                builder.clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(clientProperties.isRequireConsent())
                        .build());

                repository.save(builder.build());
                log.info("###### Default OAuth2 client created: {}", clientId);
            }
        };
    }

}