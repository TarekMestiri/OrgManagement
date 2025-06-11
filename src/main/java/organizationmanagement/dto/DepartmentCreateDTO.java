package organizationmanagement.dto;

import java.util.UUID;

public class DepartmentCreateDTO {
    private String name;
    private UUID organizationId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
