package organizationmanagement.repository;

import organizationmanagement.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByOrganizationId(UUID organizationId);
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
