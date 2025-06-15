package organizationmanagement.service;

import organizationmanagement.client.SurveyServiceClient;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.exception.*;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final UserServiceClient userServiceClient;
    private final SurveyServiceClient surveyServiceClient;

    // Existing methods (unchanged)
    public List<Team> getAll() {
        return teamRepository.findAll();
    }

    public Team getById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
    }

    public void delete(UUID id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team not found with id: " + id);
        }
        teamRepository.deleteById(id);
    }

    public List<Team> getByDepartmentId(UUID departmentId) {
        return teamRepository.findByDepartmentId(departmentId);
    }

    public Team createUnderDepartment(UUID deptId, Team team) {
        validateTeamName(team.getName());

        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + deptId));

        boolean exists = teamRepository.existsByNameAndDepartmentId(team.getName().trim(), deptId);
        if (exists) {
            throw new BadRequestException("A team with the name '" + team.getName().trim() + "' already exists in this department.");
        }

        team.setDepartment(department);
        return teamRepository.save(team);
    }

    public Team update(UUID id, UUID departmentId, Team updatedTeam) {
        validateTeamName(updatedTeam.getName());

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        boolean exists = teamRepository.existsByNameAndDepartmentId(updatedTeam.getName().trim(), departmentId);
        if (exists && !existingTeam.getName().equalsIgnoreCase(updatedTeam.getName().trim())) {
            throw new BadRequestException("A team with the name '" + updatedTeam.getName().trim() + "' already exists in this department.");
        }

        existingTeam.setName(updatedTeam.getName().trim());
        existingTeam.setDepartment(department);

        return teamRepository.save(existingTeam);
    }

    // NEW: Organization-scoped methods

    public List<Team> getAllByOrganization(UUID organizationId) {
        return teamRepository.findByDepartmentOrganizationId(organizationId);
    }

    public Team getByIdAndOrganization(UUID id, UUID organizationId) {
        return teamRepository.findByIdAndDepartmentOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team not found with id: " + id + " in organization: " + organizationId));
    }

    public void deleteByIdAndOrganization(UUID id, UUID organizationId) {
        Team team = getByIdAndOrganization(id, organizationId);
        teamRepository.delete(team);
    }

    public List<Team> getByDepartmentIdAndOrganization(UUID departmentId, UUID organizationId) {
        // First verify the department belongs to the organization
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + departmentId + " in organization: " + organizationId));

        return teamRepository.findByDepartmentId(departmentId);
    }

    public Team createUnderDepartmentInOrganization(UUID deptId, Team team, UUID organizationId) {
        validateTeamName(team.getName());

        // Verify department belongs to the organization
        Department department = departmentRepository.findByIdAndOrganizationId(deptId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + deptId + " in organization: " + organizationId));

        // Check for duplicate team name within the department
        boolean exists = teamRepository.existsByNameAndDepartmentId(team.getName().trim(), deptId);
        if (exists) {
            throw new BadRequestException("A team with the name '" + team.getName().trim() + "' already exists in this department.");
        }

        team.setDepartment(department);
        return teamRepository.save(team);
    }

    public Team updateInOrganization(UUID id, UUID departmentId, Team updatedTeam, UUID organizationId) {
        validateTeamName(updatedTeam.getName());

        // Verify team exists in the organization
        Team existingTeam = getByIdAndOrganization(id, organizationId);

        // Verify new department belongs to the organization
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + departmentId + " in organization: " + organizationId));

        // Check for duplicate team name within the new department
        boolean exists = teamRepository.existsByNameAndDepartmentId(updatedTeam.getName().trim(), departmentId);
        if (exists && !existingTeam.getName().equalsIgnoreCase(updatedTeam.getName().trim())) {
            throw new BadRequestException("A team with the name '" + updatedTeam.getName().trim() + "' already exists in this department.");
        }

        existingTeam.setName(updatedTeam.getName().trim());
        existingTeam.setDepartment(department);

        return teamRepository.save(existingTeam);
    }

    // Helper method for organization validation
    private void validateOrganizationAccess(UUID teamId, UUID organizationId) {
        boolean exists = teamRepository.existsByIdAndDepartmentOrganizationId(teamId, organizationId);
        if (!exists) {
            throw new ResourceNotFoundException(
                    "Team not found with id: " + teamId + " in organization: " + organizationId);
        }
    }

    private void validateTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Team name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Team name must be between 2 and 100 characters.");
        }
    }


    // Organization-scoped versions (also without ServiceUnavailableException)

    @Transactional
    public void assignUserToTeamInOrganization(UUID teamId, UUID userId, UUID organizationId) {
        // 1. Find team and verify it exists in the organization
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team not found with id " + teamId + " in organization " + organizationId));

        // 2. Verify user exists (EXACTLY like DepartmentService)
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(userId);
        if (userExistsResponse.getBody() == null || !userExistsResponse.getBody()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // 3. Check for existing assignment (EXACTLY like DepartmentService)
        if (team.getUserIds().contains(userId)) {
            throw new BadRequestException("User is already assigned to this team");
        }

        // 4. Perform assignment (EXACTLY like DepartmentService)
        team.getUserIds().add(userId);
        teamRepository.save(team);
    }

    @Transactional
    public void removeUserFromTeamInOrganization(UUID teamId, UUID userId, UUID organizationId) {
        // 1. Find team and verify it exists in the organization
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team not found with id " + teamId + " in organization " + organizationId));

        // 2. Verify user is actually assigned
        if (!team.getUserIds().contains(userId)) {
            throw new BadRequestException("User is not assigned to this team");
        }

        // 3. Perform removal
        team.getUserIds().remove(userId);
        teamRepository.save(team);
    }

    @Transactional
    public void assignSurveyToTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId) {
        // 1. Find team and verify it exists in the organization
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team not found with id " + teamId + " in organization " + organizationId));

        // 2. Verify survey exists using Feign client
        ResponseEntity<Boolean> surveyExistsResponse = surveyServiceClient.surveyExists(surveyId);
        if (surveyExistsResponse.getBody() == null || !surveyExistsResponse.getBody()) {
            throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
        }

        // Optional: Verify survey belongs to organization if needed
    /*
    ResponseEntity<Boolean> surveyInOrgResponse = surveyServiceClient.surveyExistsInOrganization(surveyId, organizationId);
    if (surveyInOrgResponse.getBody() == null || !surveyInOrgResponse.getBody()) {
        throw new BadRequestException("Survey does not belong to this organization");
    }
    */

        // 3. Check for existing assignment
        if (team.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is already assigned to this team");
        }

        // 4. Perform assignment
        team.getSurveyIds().add(surveyId);
        teamRepository.save(team);
    }

    @Transactional
    public void removeSurveyFromTeamInOrganization(UUID teamId, UUID surveyId, UUID organizationId) {
        // 1. Find team and verify it exists in the organization
        Team team = teamRepository.findByIdAndDepartmentOrganizationId(teamId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team not found with id " + teamId + " in organization " + organizationId));

        // 2. Verify survey is actually assigned
        if (!team.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is not assigned to this team");
        }

        // 3. Perform removal
        team.getSurveyIds().remove(surveyId);
        teamRepository.save(team);
    }
}