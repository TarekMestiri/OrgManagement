package organizationmanagement.repository;

import organizationmanagement.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByDepartmentId(UUID departmentId);
    boolean existsByNameAndDepartmentId(String name, UUID departmentId);
}
