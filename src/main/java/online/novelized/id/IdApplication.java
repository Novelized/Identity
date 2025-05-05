package online.novelized.id;

import online.novelized.id.config.CorsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CorsProperties.class)
public class IdApplication {

    private static final Logger log = LoggerFactory.getLogger(IdApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }

}