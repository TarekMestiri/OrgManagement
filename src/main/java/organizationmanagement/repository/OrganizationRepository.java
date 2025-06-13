package organizationmanagement.repository;

import organizationmanagement.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
// public List<Organization> getSubOrganizations(UUID parentId);
// public List<Organization> getByParentOrganization(UUID parentId);
// public boolean canUserAccessOrganization(UUID userId, UUID orgId);
}