package organizationmanagement.dto;

import java.util.UUID;

public class DepartmentDTO {
    private UUID id;
    private String name;
    private OrganizationDTO organization;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OrganizationDTO getOrganization() { return organization; }
    public void setOrganization(OrganizationDTO organization) { this.organization = organization; }
}
