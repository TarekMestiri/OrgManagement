package organizationmanagement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import organizationmanagement.dto.UserAssignmentDTO;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    // Assign user to a specific department
    @PostMapping("/api/users/{userId}/assign-to-department")
    ResponseEntity<Void> assignUserToDepartment(
            @PathVariable("userId") UUID userId,
            @RequestBody UserAssignmentDTO assignmentDTO
    );

    // Assign user to a specific team
    @PostMapping("/api/users/{userId}/assign-to-team")
    ResponseEntity<Void> assignUserToTeam(
            @PathVariable("userId") UUID userId,
            @RequestBody UserAssignmentDTO assignmentDTO
    );

    // Remove user from a department
    @DeleteMapping("/api/users/{userId}/remove-from-department")
    ResponseEntity<Void> removeUserFromDepartment(
            @PathVariable("userId") UUID userId,
            @RequestBody UserAssignmentDTO assignmentDTO
    );

    // Remove user from a team
    @DeleteMapping("/api/users/{userId}/remove-from-team")
    ResponseEntity<Void> removeUserFromTeam(
            @PathVariable("userId") UUID userId,
            @RequestBody UserAssignmentDTO assignmentDTO
    );

    @GetMapping("/api/users/{userId}/exists")
    ResponseEntity<Boolean> userExists(@PathVariable("userId") UUID userId);
}