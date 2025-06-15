package organizationmanagement.service;

import organizationmanagement.client.SurveyServiceClient;
import organizationmanagement.client.UserServiceClient;
import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final UserServiceClient userServiceClient;
    private final SurveyServiceClient surveyServiceClient;

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

    private void validateDepartmentName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Department name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Department name must be between 2 and 100 characters.");
        }
    }

    // Organization-scoped versions of assignment methods

    public void assignUserToDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        // Verify user exists using Feign client
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(userId);
        if (userExistsResponse.getBody() == null || !userExistsResponse.getBody()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Check if user is already assigned
        if (department.getUserIds().contains(userId)) {
            throw new BadRequestException("User is already assigned to this department");
        }

        department.getUserIds().add(userId);
        departmentRepository.save(department);
    }

    public void removeUserFromDepartmentInOrganization(UUID departmentId, UUID userId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        if (!department.getUserIds().contains(userId)) {
            throw new BadRequestException("User is not assigned to this department");
        }

        department.getUserIds().remove(userId);
        departmentRepository.save(department);
    }
    public void assignSurveyToDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        // Verify survey exists using Feign client
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

        // Check if survey is already assigned
        if (department.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is already assigned to this department");
        }

        department.getSurveyIds().add(surveyId);
        departmentRepository.save(department);
    }

    public void removeSurveyFromDepartmentInOrganization(UUID departmentId, UUID surveyId, UUID organizationId) {
        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id " + departmentId + " in organization " + organizationId));

        if (!department.getSurveyIds().contains(surveyId)) {
            throw new BadRequestException("Survey is not assigned to this department");
        }

        department.getSurveyIds().remove(surveyId);
        departmentRepository.save(department);
    }
}