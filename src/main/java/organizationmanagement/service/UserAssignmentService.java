package organizationmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.dto.UserAssignmentDTO;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.TeamService;
import organizationmanagement.service.OrganizationService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAssignmentService {

    private final UserServiceClient userServiceClient;
    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final TeamService teamService;

    public void assignUserToDepartment(UUID userId, UUID organizationId, UUID departmentId) {
        // Validate organization exists
        if (!organizationService.exists(organizationId)) {
            throw new IllegalArgumentException("Organization not found: " + organizationId);
        }

        // Validate department exists and belongs to organization
        var department = departmentService.getById(departmentId);
        if (!department.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Department does not belong to the specified organization");
        }

        // Validate user exists
        if (!userServiceClient.userExists(userId).getBody()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        UserAssignmentDTO assignmentDTO = new UserAssignmentDTO(
                departmentId,
                null,  // teamId is null for department assignments
                "DEPARTMENT"
        );

        userServiceClient.assignUserToDepartment(userId, assignmentDTO);
        log.info("User {} assigned to department {} in organization {}", userId, departmentId, organizationId);
    }

    public void assignUserToTeam(UUID userId, UUID organizationId, UUID teamId) {
        // Validate team exists
        var team = teamService.getById(teamId);
        var department = team.getDepartment();

        // Validate team belongs to organization
        if (!department.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Team does not belong to the specified organization");
        }

        // Validate user exists
        if (!userServiceClient.userExists(userId).getBody()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        UserAssignmentDTO assignmentDTO = new UserAssignmentDTO(
                department.getId(),
                teamId,
                "TEAM"
        );

        userServiceClient.assignUserToTeam(userId, assignmentDTO);
        log.info("User {} assigned to team {} in department {} in organization {}",
                userId, teamId, department.getId(), organizationId);
    }

    public void removeUserFromDepartment(UUID userId, UUID organizationId, UUID departmentId) {
        // Validate organization exists
        if (!organizationService.exists(organizationId)) {
            throw new IllegalArgumentException("Organization not found: " + organizationId);
        }

        // Validate department exists and belongs to organization
        var department = departmentService.getById(departmentId);
        if (!department.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Department does not belong to the specified organization");
        }

        // Validate user exists
        if (!userServiceClient.userExists(userId).getBody()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        userServiceClient.removeUserFromDepartment(userId, new UserAssignmentDTO(
                departmentId,
                null,
                "DEPARTMENT"
        ));
        log.info("User {} removed from department {} in organization {}", userId, departmentId, organizationId);
    }

    public void removeUserFromTeam(UUID userId, UUID organizationId, UUID teamId) {
        // Validate team exists
        var team = teamService.getById(teamId);
        var department = team.getDepartment();

        // Validate team belongs to organization
        if (!department.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Team does not belong to the specified organization");
        }

        // Validate user exists
        if (!userServiceClient.userExists(userId).getBody()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        userServiceClient.removeUserFromTeam(userId, new UserAssignmentDTO(
                department.getId(),
                teamId,
                "TEAM"
        ));
        log.info("User {} removed from team {} in department {} in organization {}",
                userId, teamId, department.getId(), organizationId);
    }
}