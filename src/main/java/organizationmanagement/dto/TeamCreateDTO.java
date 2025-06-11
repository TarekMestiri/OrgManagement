package organizationmanagement.dto;

import java.util.UUID;

public class TeamCreateDTO {
    private String name;
    private UUID departmentId;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
