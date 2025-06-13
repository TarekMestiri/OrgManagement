package organizationmanagement.repository;

import organizationmanagement.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByOrganizationId(UUID organizationId);
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    // New method: Find department by ID within a specific organization
    Optional<Department> findByIdAndOrganizationId(UUID id, UUID organizationId);

    // New method: Check if department name exists in organization excluding a specific ID (for updates)
    boolean existsByNameAndOrganizationIdAndIdNot(String name, UUID organizationId, UUID excludeId);
}