package gov.dhs.cbp.reference.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080" + contextPath).description("Local Development"),
                        new Server().url("https://api-dev.cbp.dhs.gov/reference-data").description("Development Environment"),
                        new Server().url("https://api-staging.cbp.dhs.gov/reference-data").description("Staging Environment"),
                        new Server().url("https://api.cbp.dhs.gov/reference-data").description("Production Environment")
                ))
                .addSecurityItem(new SecurityRequirement().addList("OAuth2"))
                .addSecurityItem(new SecurityRequirement().addList("ApiKey"))
                .components(new Components()
                        .addSecuritySchemes("OAuth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 Authentication via Keycloak")
                                .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
                                        .authorizationCode(new io.swagger.v3.oas.models.security.OAuthFlow()
                                                .authorizationUrl("http://localhost:8085/realms/cbp-reference-data/protocol/openid-connect/auth")
                                                .tokenUrl("http://localhost:8085/realms/cbp-reference-data/protocol/openid-connect/token")
                                                .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                        .addString("read", "Read access to reference data")
                                                        .addString("write", "Write access to reference data")
                                                        .addString("admin", "Administrative access")))))
                        .addSecuritySchemes("ApiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .description("API Key Authentication")
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")));
    }
    
    private Info apiInfo() {
        return new Info()
                .title("CBP Reference Data Service API")
                .description("""
                        ## Overview
                        
                        The CBP Reference Data Service provides centralized access to canonical reference data including:
                        
                        - **Countries**: ISO 3166-1 country codes and CBP-specific country data
                        
                        ## Features
                        
                        ### Bitemporal Data Model
                        - **Valid Time**: When the data is effective in the real world
                        - **System Time**: When the data was recorded in the system
                        - Full history preservation with no hard deletes
                        - Point-in-time queries with `asOf` parameter
                        
                        ### Caching & Performance
                        - HTTP caching with ETags and conditional requests
                        - Redis caching for frequently accessed translations
                        - Optimized pagination with HATEOAS links
                        
                        ### Data Governance
                        - Change request workflow with approval processes
                        - Audit trail for all modifications
                        - Metadata tracking and validation
                        
                        ### Event-Driven Architecture
                        - Transactional outbox pattern for reliable event publishing
                        - Kafka event streaming for real-time data synchronization
                        - Schema registry for event compatibility
                        
                        ## Authentication
                        
                        This API supports two authentication methods:
                        
                        1. **OAuth2/OIDC** via Keycloak (recommended for interactive applications)
                        2. **API Key** authentication via `X-API-Key` header (for service-to-service)
                        
                        ## Authorization
                        
                        - **Anonymous**: Read-only access to publicly available reference data
                        - **USER**: Submit change requests, view own requests
                        - **APPROVER**: Review and approve/reject change requests
                        - **ADMIN**: Full access including system administration
                        
                        ## Rate Limiting
                        
                        - **Anonymous**: 100 requests per minute
                        - **Authenticated**: 1000 requests per minute
                        - **Service accounts**: 5000 requests per minute
                        
                        ## Data Formats
                        
                        All timestamps use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
                        
                        All dates use ISO 8601 date format: `YYYY-MM-DD`
                        
                        ## Error Handling
                        
                        The API uses RFC 7807 Problem Details format for error responses:
                        
                        ```json
                        {
                          "type": "https://api.cbp.dhs.gov/problems/validation-error",
                          "title": "Validation Failed",
                          "status": 400,
                          "detail": "The country code 'XX' is not valid",
                          "instance": "/v1/countries/XX",
                          "traceId": "abc123"
                        }
                        ```
                        
                        ## Versioning
                        
                        This API uses URL path versioning (e.g., `/v1/countries`). 
                        
                        Breaking changes will increment the major version number.
                        """)
                .version(appVersion)
                .contact(new Contact()
                        .name("CBP Reference Data Team")
                        .email("cbp-reference-data@dhs.gov")
                        .url("https://github.com/CBP-IT/reference-data"))
                .license(new License()
                        .name("U.S. Government Work")
                        .url("https://www.usa.gov/government-works"))
                .termsOfService("https://www.cbp.gov/about/privacy-policy");
    }
}