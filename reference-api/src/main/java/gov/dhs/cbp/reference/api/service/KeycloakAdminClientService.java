package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.config.KeycloakAdminProperties;
import gov.dhs.cbp.reference.api.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeycloakAdminClientService {

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClientService(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm(properties.realm())
                .grantType("password")
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .username(properties.username())
                .password(properties.password())
                .build();
    }

    public List<UserDto> getStewards() {
        log.info("Attempting to fetch data stewards from Keycloak realm '{}'", properties.realm());
        try {
            RoleResource roleResource = keycloak.realm(properties.realm()).roles().get("DATA_STEWARD");
            if (roleResource == null) {
                log.warn("DATA_STEWARD role not found in realm '{}'", properties.realm());
                return Collections.emptyList();
            }
            RoleRepresentation dataStewardRole = roleResource.toRepresentation();
            log.info("Found DATA_STEWARD role: {}", dataStewardRole.getName());

            Set<UserRepresentation> users = roleResource.getRoleUserMembers();
            log.info("Found {} users with the DATA_STEWARD role", users.size());

            return users.stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get data stewards from Keycloak. URL: {}, Realm: {}", properties.serverUrl(), properties.realm(), e);
            throw new RuntimeException("Failed to get data stewards from Keycloak", e);
        }
    }

    public List<String> getUserOwnershipRoleNames(String userId) {
        log.info("Fetching ownership roles for user '{}'", userId);
        try {
            return keycloak.realm(properties.realm()).users().get(userId).roles().clientLevel(properties.clientId()).listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(roleName -> roleName.endsWith("-owner"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get ownership roles for user '{}'", userId, e);
            return Collections.emptyList();
        }
    }

    public void updateUserOwnershipRoles(String userId, List<String> permissions) {
        log.info("Updating ownership roles for user '{}' to: {}", userId, permissions);
        try {
            List<RoleRepresentation> availableRoles = keycloak.realm(properties.realm()).clients().get(properties.clientId()).roles().list();
            List<RoleRepresentation> rolesToAdd = availableRoles.stream()
                .filter(role -> permissions.contains(role.getName()))
                .collect(Collectors.toList());

            List<RoleRepresentation> currentRoles = keycloak.realm(properties.realm()).users().get(userId).roles().clientLevel(properties.clientId()).listEffective();
            List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                .filter(role -> !permissions.contains(role.getName()) && role.getName().endsWith("-owner"))
                .collect(Collectors.toList());

            keycloak.realm(properties.realm()).users().get(userId).roles().clientLevel(properties.clientId()).add(rolesToAdd);
            keycloak.realm(properties.realm()).users().get(userId).roles().clientLevel(properties.clientId()).remove(rolesToRemove);
        } catch (Exception e) {
            log.error("Failed to update ownership roles for user '{}'", userId, e);
            throw new RuntimeException("Failed to update ownership roles", e);
        }
    }

    private UserDto toUserDto(UserRepresentation userRepresentation) {
        return new UserDto(
            userRepresentation.getId(),
            userRepresentation.getUsername(),
            userRepresentation.getFirstName(),
            userRepresentation.getLastName()
        );
    }
}
