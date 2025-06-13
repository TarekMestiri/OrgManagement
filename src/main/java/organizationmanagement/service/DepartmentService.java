package organizationmanagement.service;

import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;

    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    public Department getById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id " + id);
        }
        departmentRepository.deleteById(id);
    }

    public List<Department> getByOrganizationId(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    // New method for organization-scoped access
    public List<Department> getAllByOrganization(UUID organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }

    // New method for organization-scoped access
    public Department getByIdAndOrganization(UUID id, UUID organizationId) {
        return departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id + " in organization " + organizationId));
    }

    // New method for organization-scoped deletion
    public void deleteByIdAndOrganization(UUID id, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id + " in organization " + organizationId));

        departmentRepository.delete(department);
    }

    public Department createUnderOrganization(UUID orgId, Department dept) {
        validateDepartmentName(dept.getName());

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id " + orgId));

        boolean exists = departmentRepository.existsByNameAndOrganizationId(dept.getName().trim(), orgId);
        if (exists) {
            throw new BadRequestException("A department with the name '" + dept.getName().trim() + "' already exists in this organization.");
        }

        dept.setOrganization(org);
        return departmentRepository.save(dept);
    }

    public Department update(Department dept) {
        validateDepartmentName(dept.getName());

        if (dept.getId() == null || !departmentRepository.existsById(dept.getId())) {
            throw new ResourceNotFoundException("Cannot update department. Department not found with id " + dept.getId());
        }

        return departmentRepository.save(dept);
    }

    // New method for organization-scoped update
    public Department updateInOrganization(UUID id, Department dept, UUID organizationId) {
        validateDepartmentName(dept.getName());

        Department existing = departmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id + " in organization " + organizationId));

        // Check for duplicate names within the same organization (excluding current department)
        boolean nameExists = departmentRepository.existsByNameAndOrganizationIdAndIdNot(dept.getName().trim(), organizationId, id);
        if (nameExists) {
            throw new BadRequestException("A department with the name '" + dept.getName().trim() + "' already exists in this organization.");
        }

        existing.setName(dept.getName());
        return departmentRepository.save(existing);
    }

    private void validateDepartmentName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Department name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Department name must be between 2 and 100 characters.");
        }
    }
}