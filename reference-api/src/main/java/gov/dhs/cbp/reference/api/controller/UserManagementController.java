package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.UserDto;
import gov.dhs.cbp.reference.api.service.KeycloakAdminClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/user-management")
@Tag(name = "User Management", description = "Operations for managing user roles and permissions")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final KeycloakAdminClientService keycloakAdminClientService;

    public UserManagementController(KeycloakAdminClientService keycloakAdminClientService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    @GetMapping("/stewards")
    @Operation(summary = "Get all Data Stewards",
               description = "Retrieves a list of all users who have the DATA_STEWARD realm role.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved data stewards")
    public ResponseEntity<List<UserDto>> getStewards() {
        return ResponseEntity.ok(keycloakAdminClientService.getStewards());
    }

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get a user's ownership permissions",
               description = "Retrieves the list of ownership-related client roles for a specific user.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user permissions")
    public ResponseEntity<List<String>> getUserPermissions(@PathVariable String userId) {
        return ResponseEntity.ok(keycloakAdminClientService.getUserOwnershipRoleNames(userId));
    }

    @PutMapping("/users/{userId}/permissions")
    @Operation(summary = "Update a user's ownership permissions",
               description = "Sets the user's ownership roles to match the provided list.")
    @ApiResponse(responseCode = "204", description = "Permissions updated successfully")
    public ResponseEntity<Void> updateUserPermissions(@PathVariable String userId, @RequestBody List<String> permissions) {
        keycloakAdminClientService.updateUserOwnershipRoles(userId, permissions);
        return ResponseEntity.noContent().build();
    }
}
