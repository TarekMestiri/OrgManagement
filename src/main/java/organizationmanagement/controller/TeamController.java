package organizationmanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.TeamCreateDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.TeamService;
import organizationmanagement.utils.OrganizationContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final DepartmentService departmentService;
    private final OrganizationContextUtil organizationContextUtil;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<TeamDTO>> getAll() {
        List<TeamDTO> teams;

        if (organizationContextUtil.isRootAdmin()) {
            teams = teamService.getAll().stream()
                    .map(this::convertToDTO)
                    .toList();
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teams = teamService.getAllByOrganization(organizationId).stream()
                    .map(this::convertToDTO)
                    .toList();
        }

        return ResponseEntity.ok(teams);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_CREATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> create(@RequestBody TeamCreateDTO teamDto) {
        if (teamDto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID must be provided to create a team.");
        }

        TeamDTO createdTeam;
        Team teamEntity = convertToEntity(teamDto);

        if (organizationContextUtil.isRootAdmin()) {
            // Root admin can create teams in any organization
            // The organization context should be derived from the department
            Team saved = teamService.createUnderDepartment(teamDto.getDepartmentId(), teamEntity);
            createdTeam = convertToDTO(saved);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            // Verify department belongs to the current organization
            Team saved = teamService.createUnderDepartmentInOrganization(
                    teamDto.getDepartmentId(), teamEntity, organizationId);
            createdTeam = convertToDTO(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> getById(@PathVariable UUID id) {
        TeamDTO team;

        if (organizationContextUtil.isRootAdmin()) {
            Team teamEntity = teamService.getById(id);
            team = convertToDTO(teamEntity);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Team teamEntity = teamService.getByIdAndOrganization(id, organizationId);
            team = convertToDTO(teamEntity);
        }

        return ResponseEntity.ok(team);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_DELETE','SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            teamService.delete(id);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teamService.deleteByIdAndOrganization(id, organizationId);
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<TeamDTO> update(@PathVariable UUID id, @RequestBody TeamCreateDTO teamDto) {
        if (teamDto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID must be provided to update a team.");
        }

        TeamDTO updatedTeam;
        Team updatedTeamEntity = convertToEntity(teamDto);

        if (organizationContextUtil.isRootAdmin()) {
            Team updated = teamService.update(id, teamDto.getDepartmentId(), updatedTeamEntity);
            updatedTeam = convertToDTO(updated);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Team updated = teamService.updateInOrganization(
                    id, teamDto.getDepartmentId(), updatedTeamEntity, organizationId);
            updatedTeam = convertToDTO(updated);
        }

        return ResponseEntity.ok(updatedTeam);
    }

    // Additional endpoint to get teams by department within organization scope
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<TeamDTO>> getTeamsByDepartment(@PathVariable UUID departmentId) {
        List<TeamDTO> teams;

        if (organizationContextUtil.isRootAdmin()) {
            teams = teamService.getByDepartmentId(departmentId).stream()
                    .map(this::convertToDTO)
                    .toList();
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            teams = teamService.getByDepartmentIdAndOrganization(departmentId, organizationId).stream()
                    .map(this::convertToDTO)
                    .toList();
        }

        return ResponseEntity.ok(teams);
    }

    // Mapping methods

    private TeamDTO convertToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());

        Department dept = team.getDepartment();
        if (dept != null) {
            DepartmentDTO deptDto = new DepartmentDTO();
            deptDto.setId(dept.getId());
            deptDto.setName(dept.getName());
            dto.setDepartment(deptDto);
        }
        return dto;
    }

    private Team convertToEntity(TeamCreateDTO dto) {
        Team team = new Team();
        team.setName(dto.getName());
        // Note: Department will be set in the service layer to ensure organization scope
        return team;
    }
}