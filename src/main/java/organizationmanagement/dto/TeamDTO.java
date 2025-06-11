package organizationmanagement.dto;

import java.util.UUID;

public class TeamDTO {
    private UUID id;
    private String name;
    private DepartmentDTO department;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DepartmentDTO getDepartment() { return department; }
    public void setDepartment(DepartmentDTO department) { this.department = department; }
}
