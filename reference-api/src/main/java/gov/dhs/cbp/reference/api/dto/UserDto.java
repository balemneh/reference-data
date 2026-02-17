package gov.dhs.cbp.reference.api.dto;

public record UserDto(
    String id,
    String username,
    String firstName,
    String lastName
) {}
