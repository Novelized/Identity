package online.novelized.id.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides custom Flyway configuration, specifically for development purposes.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Defines a Flyway migration strategy that cleans the database before migrating.
     * This strategy is only active if the 'app.flyway.recreate-db' property is set to 'true'.
     * @WARNING: This will wipe all data in the configured Flyway schemas on startup.
     *
     * @return The FlywayMigrationStrategy bean.
     */
    @Bean
    @ConditionalOnProperty(name = "app.flyway.recreate-db", havingValue = "true")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        log.warn("!!! Activated 'cleanMigrateStrategy' for Flyway. Database will be cleaned and recreated on startup! !!!");
        return flyway -> {
            log.info("Executing Flyway clean...");
            flyway.clean();
            log.info("Executing Flyway migrate...");
            flyway.migrate();
            log.info("Flyway clean and migrate finished.");
        };
    }
} 