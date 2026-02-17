package gov.dhs.cbp.reference.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfiguration.class);

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Creating custom JwtDecoder");
        return NimbusJwtDecoder.withJwkSetUri("http://keycloak:8080/realms/reference-data/protocol/openid-connect/certs").build();
    }
}
