package io.store.ua.controllers;

import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.models.dto.RegularUserDTO;
import io.store.ua.service.RegularUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final RegularUserService regularUserService;

    @GetMapping
    public ResponseEntity<?> getUser() {
        return RegularUserService.getCurrentlyAuthenticatedUser()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND.value()).build());
    }

    @GetMapping("/find")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
    public ResponseEntity<?> findUsers(@RequestParam(value = "username", required = false) String usernamePrefix,
                                       @RequestParam(value = "email", required = false) String emailPart,
                                       @RequestParam(value = "role", required = false) List<Role> roles,
                                       @RequestParam(value = "status", required = false) List<Status> statuses,
                                       @RequestParam(value = "isOnline", required = false) Boolean isOnline,
                                       @RequestParam(value = "pageSize") int pageSize,
                                       @RequestParam(value = "page") int page) {
        return ResponseEntity.ok(regularUserService.findBy(usernamePrefix, emailPart, roles, statuses, isOnline, pageSize, page));
    }

    @GetMapping("/find/byStatus")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
    public ResponseEntity<?> findUsersByStatus(@RequestParam("status") Status status,
                                               @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
                                               @RequestParam(value = "page", defaultValue = "1") int page) {
        return ResponseEntity.ok(regularUserService.findByStatus(status, pageSize, page));
    }

    @GetMapping("/find/byRole")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
    public ResponseEntity<?> findUsersByRole(@RequestParam("role") Role role,
                                             @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
                                             @RequestParam(value = "page", defaultValue = "1") int page) {
        return ResponseEntity.ok(regularUserService.findByRole(role, pageSize, page));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
    public ResponseEntity<?> addUser(@RequestBody RegularUserDTO regularUser) {
        return ResponseEntity.ok(regularUserService.save(regularUser));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
    public ResponseEntity<?> updateUser(@RequestBody RegularUserDTO regularUser) {
        return ResponseEntity.ok(regularUserService.update(regularUser));
    }
}
