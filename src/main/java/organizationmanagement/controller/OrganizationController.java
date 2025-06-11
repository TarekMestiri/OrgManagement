package organizationmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.dto.TeamDTO;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import organizationmanagement.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_123','ADMIN_ROOT123')")
    public List<Organization> getAll() {
        return organizationService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody Organization organization) {
        return organizationService.create(organization);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable UUID id) {
        boolean exists = organizationService.exists(id);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{id}")
    public Organization getById(@PathVariable UUID id) {
        return organizationService.getById(id);
    }

    @PutMapping("/{id}")
    public Organization update(@PathVariable UUID id, @RequestBody Organization organization) {
        return organizationService.update(id, organization);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        organizationService.delete(id);
    }

    @GetMapping("/{id}/children")
    public ChildrenResponse getChildren(@PathVariable UUID id) {
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

        return new ChildrenResponse(departments, teams);
    }

    private OrganizationDTO toOrganizationDTO(Organization org) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        return dto;
    }

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