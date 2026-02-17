package gov.dhs.cbp.reference.api.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("securityUtils")
public class SecurityUtils {

    private static final String KEYCLOAK_CLIENT_ID = "reference-admin-ui";
    private static final String OKTA_GROUPS_CLAIM = "groups";
    private static final String KEYCLOAK_IDENTIFIER = "/realms/";

    public boolean hasClientRoleForEntityType(String entityType, Authentication authentication) {
        String requiredRole = entityType.toLowerCase() + "-owner";
        List<String> roles = getRolesFromToken(authentication);
        return roles.contains(requiredRole);
    }

    public List<String> getOwnedEntityTypes(Authentication authentication) {
        return getRolesFromToken(authentication).stream()
            .filter(role -> role.endsWith("-owner"))
            .map(role -> role.replace("-owner", "").toUpperCase())
            .collect(Collectors.toList());
    }

    private List<String> getRolesFromToken(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Collections.emptyList();
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        URL issuer = jwt.getIssuer();

        if (issuer != null && issuer.toString().contains(KEYCLOAK_IDENTIFIER)) {
            return getRolesFromKeycloakToken(jwt);
        } else {
            return getRolesFromOktaToken(jwt);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getRolesFromKeycloakToken(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess != null && resourceAccess.containsKey(KEYCLOAK_CLIENT_ID)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(KEYCLOAK_CLIENT_ID);
            List<String> clientRoles = (List<String>) clientAccess.get("roles");
            if (clientRoles != null) {
                return clientRoles;
            }
        }
        return Collections.emptyList();
    }

    private List<String> getRolesFromOktaToken(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(OKTA_GROUPS_CLAIM);
        return groups != null ? groups : Collections.emptyList();
    }
}
