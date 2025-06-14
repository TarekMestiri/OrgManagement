package organizationmanagement.controller;

import organizationmanagement.dto.DepartmentCreateDTO;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import organizationmanagement.utils.OrganizationContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;
    private final OrganizationService organizationService;
    private final OrganizationContextUtil organizationContextUtil;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        List<DepartmentDTO> departments;

        if (organizationContextUtil.isRootAdmin()) {
            departments = service.getAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            departments = service.getAllByOrganization(organizationId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(departments);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_CREATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> create(@RequestBody DepartmentCreateDTO deptDto) {
        DepartmentDTO createdDepartment;

        if (organizationContextUtil.isRootAdmin()) {
            if (deptDto.getOrganizationId() == null) {
                throw new BadRequestException("Organization ID is required for sys admin department creation");
            }
            Department deptEntity = convertToEntity(deptDto);
            Department saved = service.createUnderOrganization(deptDto.getOrganizationId(), deptEntity);
            createdDepartment = convertToDTO(saved);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            deptDto.setOrganizationId(organizationId);
            Department deptEntity = convertToEntity(deptDto);
            Department saved = service.createUnderOrganization(organizationId, deptEntity);
            createdDepartment = convertToDTO(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_READ','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> getById(@PathVariable UUID id) {
        DepartmentDTO department;

        if (organizationContextUtil.isRootAdmin()) {
            Department dept = service.getById(id);
            if (dept == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }
            department = convertToDTO(dept);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Department dept = service.getByIdAndOrganization(id, organizationId);
            if (dept == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }
            department = convertToDTO(dept);
        }

        return ResponseEntity.ok(department);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_UPDATE','SYS_ADMIN_ROOT')")
    public ResponseEntity<DepartmentDTO> update(@PathVariable UUID id, @RequestBody DepartmentCreateDTO deptDto) {
        DepartmentDTO updatedDepartment;

        if (organizationContextUtil.isRootAdmin()) {
            if (deptDto.getOrganizationId() == null) {
                throw new BadRequestException("Organization ID is required for sys admin department update");
            }

            Department existing = service.getById(id);
            if (existing == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }

            existing.setName(deptDto.getName());
            Organization org = organizationService.getById(deptDto.getOrganizationId());
            if (org == null) {
                throw new BadRequestException("Organization not found with ID: " + deptDto.getOrganizationId());
            }
            existing.setOrganization(org);

            Department updated = service.update(existing);
            updatedDepartment = convertToDTO(updated);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            Department existing = service.getByIdAndOrganization(id, organizationId);
            if (existing == null) {
                throw new BadRequestException("Department not found with ID: " + id);
            }

            existing.setName(deptDto.getName());
            // Keep the same organization for non-root users
            Department updated = service.update(existing);
            updatedDepartment = convertToDTO(updated);
        }

        return ResponseEntity.ok(updatedDepartment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_DELETE','SYS_ADMIN_ROOT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (organizationContextUtil.isRootAdmin()) {
            service.delete(id);
        } else {
            UUID organizationId = organizationContextUtil.getCurrentOrganizationId();
            service.deleteByIdAndOrganization(id, organizationId);
        }

        return ResponseEntity.noContent().build();
    }

    // Mapping methods

    private DepartmentDTO convertToDTO(Department dept) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(dept.getId());
        dto.setName(dept.getName());

        Organization org = dept.getOrganization();
        if (org != null) {
            OrganizationDTO orgDto = new OrganizationDTO();
            orgDto.setId(org.getId());
            orgDto.setName(org.getName());
            dto.setOrganization(orgDto);
        }
        return dto;
    }

    private Department convertToEntity(DepartmentCreateDTO dto) {
        Department dept = new Department();
        dept.setName(dto.getName());
        if (dto.getOrganizationId() != null) {
            Organization org = organizationService.getById(dto.getOrganizationId());
            dept.setOrganization(org);
        }
        return dept;
    }
}