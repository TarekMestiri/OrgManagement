package organizationmanagement.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignmentDTO {
    private UUID departmentId;
    private UUID teamId;
    private String assignmentType; //  "DEPARTMENT", "TEAM"
}
