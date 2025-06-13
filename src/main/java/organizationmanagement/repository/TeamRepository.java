package organizationmanagement.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import organizationmanagement.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByDepartmentId(UUID departmentId);
    boolean existsByNameAndDepartmentId(String name, UUID departmentId);
    // NEW: Organization-scoped methods

    /**
     * Find all teams within a specific organization
     */
    @Query("SELECT t FROM Team t WHERE t.department.organization.id = :organizationId")
    List<Team> findByDepartmentOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find a team by ID that belongs to a specific organization
     */
    @Query("SELECT t FROM Team t WHERE t.id = :teamId AND t.department.organization.id = :organizationId")
    Optional<Team> findByIdAndDepartmentOrganizationId(@Param("teamId") UUID teamId,
                                                       @Param("organizationId") UUID organizationId);

    /**
     * Check if a team exists by ID within a specific organization
     */
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.id = :teamId AND t.department.organization.id = :organizationId")
    boolean existsByIdAndDepartmentOrganizationId(@Param("teamId") UUID teamId,
                                                  @Param("organizationId") UUID organizationId);
}
