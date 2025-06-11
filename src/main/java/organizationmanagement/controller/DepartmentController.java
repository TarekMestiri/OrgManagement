package organizationmanagement.controller;

import organizationmanagement.dto.DepartmentCreateDTO;
import organizationmanagement.dto.DepartmentDTO;
import organizationmanagement.dto.OrganizationDTO;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.service.DepartmentService;
import organizationmanagement.service.OrganizationService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public List<DepartmentDTO> getAll() {
        return service.getAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public DepartmentDTO create(@RequestBody DepartmentCreateDTO deptDto) {
        if (deptDto.getOrganizationId() == null) {
            throw new BadRequestException("Organization ID is required to create a department");
        }

        Department deptEntity = convertToEntity(deptDto);
        Department saved = service.createUnderOrganization(deptDto.getOrganizationId(), deptEntity);
        return convertToDTO(saved);
    }

    @GetMapping("/{id}")
    public DepartmentDTO getById(@PathVariable UUID id) {
        Department dept = service.getById(id);
        if (dept == null) {
            throw new BadRequestException("Department not found with ID: " + id);
        }
        return convertToDTO(dept);
    }

    @PutMapping("/{id}")
    public DepartmentDTO update(@PathVariable UUID id, @RequestBody DepartmentCreateDTO deptDto) {
        if (deptDto.getOrganizationId() == null) {
            throw new BadRequestException("Organization ID is required to update a department");
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
        return convertToDTO(updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
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
