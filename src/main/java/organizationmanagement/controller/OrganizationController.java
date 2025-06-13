package organizationmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import organizationmanagement.service.TeamService;
import organizationmanagement.service.UserAssignmentService;
import organizationmanagement.utils.OrganizationContextUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final UserAssignmentService userAssignmentService;
    private final OrganizationContextUtil organizationContextUtil;

    @GetMapping
    @PreAuthorize("hasAuthority('SYS_ADMIN_ROOT')")
    public ResponseEntity<List<Organization>> getAll() {
        List<Organization> organizations = organizationService.getAll();
        return ResponseEntity.ok(organizations);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody Organization organization) {
        return organizationService.create(organization);
    }

    @GetMapping("/{id}/exists")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_READ', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Boolean> exists(@PathVariable UUID id) {
        boolean exists;

        if (organizationContextUtil.isRootAdmin()) {
            exists = organizationService.exists(id);
        } else {
            // For regular users, check if they can access this organization
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            exists = organizationService.exists(id) && id.equals(currentOrgId);
        }

        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_READ', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Organization> getById(@PathVariable UUID id) {
        Organization organization;

        if (organizationContextUtil.isRootAdmin()) {
            organization = organizationService.getById(id);
        } else {
            // Regular users can only access their own organization
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only access your own organization");
            }
            organization = organizationService.getById(id);
        }

        return ResponseEntity.ok(organization);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Organization> update(@PathVariable UUID id, @RequestBody Organization organization) {
        Organization updatedOrganization;

        if (organizationContextUtil.isRootAdmin()) {
            updatedOrganization = organizationService.update(id, organization);
        } else {
            // Regular users can only update their own organization
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only update your own organization");
            }
            updatedOrganization = organizationService.update(id, organization);
        }

        return ResponseEntity.ok(updatedOrganization);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            organizationService.delete(id);
        } else {
            // Regular users might not be allowed to delete organizations
            // or only delete their own organization based on your business rules
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only delete your own organization");
            }
            organizationService.delete(id);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_READ', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<ChildrenResponse> getChildren(@PathVariable UUID id) {
        // First verify access to the organization
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!id.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only access children of your own organization");
            }
        }

        Organization org = organizationService.getById(id);
        OrganizationDTO orgDTO = toOrganizationDTO(org);

        List<DepartmentDTO> departments = departmentService.getByOrganizationId(id).stream()
                .map(dept -> {
                    DepartmentDTO dto = new DepartmentDTO();
                    dto.setId(dept.getId());
                    dto.setName(dept.getName());
                    dto.setOrganization(orgDTO);
                    return dto;
                })
                .collect(Collectors.toList());

        List<TeamDTO> teams = departments.stream()
                .flatMap(deptDTO -> teamService.getByDepartmentId(deptDTO.getId()).stream()
                        .map(team -> {
                            TeamDTO teamDTO = new TeamDTO();
                            teamDTO.setId(team.getId());
                            teamDTO.setName(team.getName());
                            teamDTO.setDepartment(deptDTO);
                            return teamDTO;
                        }))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ChildrenResponse(departments, teams));
    }

    // ===== USER ASSIGNMENT ENDPOINTS =====

    @PostMapping("/{organizationId}/departments/{departmentId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> assignUserToDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID userId) {

        // Check access permissions
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!organizationId.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only assign users to your own organization");
            }
        }

        try {
            userAssignmentService.assignUserToDepartment(userId, organizationId, departmentId);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for user assignment to department: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error assigning user {} to department {} in organization {}: {}", userId, departmentId, organizationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{organizationId}/teams/{teamId}/assign-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_UPDATE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> assignUserToTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {

        // Check access permissions
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!organizationId.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only assign users to your own organization");
            }
        }

        try {
            userAssignmentService.assignUserToTeam(userId, organizationId, teamId);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for user assignment to team: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error assigning user {} to team {} in organization {}: {}", userId, teamId, organizationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{organizationId}/departments/{departmentId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> removeUserFromDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID departmentId,
            @PathVariable UUID userId) {

        // Check access permissions
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!organizationId.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only remove users from your own organization");
            }
        }

        try {
            userAssignmentService.removeUserFromDepartment(userId, organizationId, departmentId);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for user removal from department: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error removing user {} from department {} in organization {}: {}", userId, departmentId, organizationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{organizationId}/teams/{teamId}/remove-user/{userId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_DELETE', 'SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> removeUserFromTeam(
            @PathVariable UUID organizationId,
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {

        // Check access permissions
        if (!organizationContextUtil.isRootAdmin()) {
            UUID currentOrgId = organizationContextUtil.getCurrentOrganizationId();
            if (!organizationId.equals(currentOrgId)) {
                throw new IllegalArgumentException("Access denied: You can only remove users from your own organization");
            }
        }

        try {
            userAssignmentService.removeUserFromTeam(userId, organizationId, teamId);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for user removal from team: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error removing user {} from team {} in organization {}: {}", userId, teamId, organizationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== HELPER METHODS =====

    private OrganizationDTO toOrganizationDTO(Organization org) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        return dto;
    }

    // ===== RESPONSE CLASSES =====

    public static class ExistsResponse {
        private boolean exists;

        public ExistsResponse(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }
    }

    public static class ChildrenResponse {
        private List<DepartmentDTO> departments;
        private List<TeamDTO> teams;

        public ChildrenResponse(List<DepartmentDTO> departments, List<TeamDTO> teams) {
            this.departments = departments;
            this.teams = teams;
        }

        public List<DepartmentDTO> getDepartments() {
            return departments;
        }

        public void setDepartments(List<DepartmentDTO> departments) {
            this.departments = departments;
        }

        public List<TeamDTO> getTeams() {
            return teams;
        }

        public void setTeams(List<TeamDTO> teams) {
            this.teams = teams;
        }
    }
}