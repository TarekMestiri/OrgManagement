package organizationmanagement.service;

import organizationmanagement.exception.BadRequestException;
import organizationmanagement.exception.ResourceNotFoundException;
import organizationmanagement.model.Department;
import organizationmanagement.model.Team;
import organizationmanagement.repository.DepartmentRepository;
import organizationmanagement.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;

    public List<Team> getAll() {
        return teamRepository.findAll();
    }

    public Team getById(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
    }

    public void delete(UUID id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team not found with id: " + id);
        }
        teamRepository.deleteById(id);
    }

    public List<Team> getByDepartmentId(UUID departmentId) {
        return teamRepository.findByDepartmentId(departmentId);
    }

    public Team createUnderDepartment(UUID deptId, Team team) {
        validateTeamName(team.getName());

        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + deptId));

        boolean exists = teamRepository.existsByNameAndDepartmentId(team.getName().trim(), deptId);
        if (exists) {
            throw new BadRequestException("A team with the name '" + team.getName().trim() + "' already exists in this department.");
        }

        team.setDepartment(department);
        return teamRepository.save(team);
    }

    public Team update(UUID id, UUID departmentId, Team updatedTeam) {
        validateTeamName(updatedTeam.getName());

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        boolean exists = teamRepository.existsByNameAndDepartmentId(updatedTeam.getName().trim(), departmentId);
        if (exists && !existingTeam.getName().equalsIgnoreCase(updatedTeam.getName().trim())) {
            throw new BadRequestException("A team with the name '" + updatedTeam.getName().trim() + "' already exists in this department.");
        }

        existingTeam.setName(updatedTeam.getName().trim());
        existingTeam.setDepartment(department);

        return teamRepository.save(existingTeam);
    }

    private void validateTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Team name must not be empty.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new BadRequestException("Team name must be between 2 and 100 characters.");
        }
    }
}
