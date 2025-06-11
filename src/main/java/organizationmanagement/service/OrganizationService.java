package organizationmanagement.service;

import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Organization;
import organizationmanagement.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 100;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-']+$");

    public List<Organization> getAll() {
        return organizationRepository.findAll();
    }


    public Organization create(Organization org) {
        validateOrganization(org);

        String normalizedName = org.getName().trim();

        boolean exists = organizationRepository.findAll().stream()
                .anyMatch(existingOrg -> existingOrg.getName() != null &&
                        existingOrg.getName().trim().equalsIgnoreCase(normalizedName));

        if (exists) {
            throw new BadRequestException("An organization with the name '" + org.getName().trim() + "' already exists.");
        }

        return organizationRepository.save(org);
    }

    public boolean exists(UUID id) {
        return organizationRepository.existsById(id);
    }

    public Organization getById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    public Organization update(UUID id, Organization updatedOrg) {
        validateOrganization(updatedOrg);

        Organization existing = getById(id);
        existing.setName(updatedOrg.getName().trim());

        return organizationRepository.save(existing);
    }

    public void delete(UUID id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Organization not found with id: " + id);
        }
        organizationRepository.deleteById(id);
    }

    private void validateOrganization(Organization org) {
        if (org.getName() == null || org.getName().trim().isEmpty()) {
            throw new BadRequestException("Organization name must not be empty.");
        }

        String trimmedName = org.getName().trim();

        if (trimmedName.length() < NAME_MIN_LENGTH || trimmedName.length() > NAME_MAX_LENGTH) {
            throw new BadRequestException("Organization name must be between " + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH + " characters.");
        }

        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new BadRequestException("Organization name contains invalid characters. Allowed: letters, numbers, spaces, hyphens, apostrophes.");
        }
    }
}