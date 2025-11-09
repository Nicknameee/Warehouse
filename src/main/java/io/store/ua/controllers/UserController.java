package io.store.ua.controllers;

import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.UserDTO;
import io.store.ua.service.UserService;
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
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUser() {
        return UserService.getCurrentlyAuthenticatedUser()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND.value()).build());
    }

    @GetMapping("/find/byRole")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> findByRole(@RequestParam("role") UserRole role,
                                        @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
                                        @RequestParam(value = "page", defaultValue = "1") int page) {
        return ResponseEntity.ok(userService.findByRole(role, pageSize, page));
    }

    @GetMapping("/find/byStatus")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> findByStatus(@RequestParam("status") UserStatus status,
                                          @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
                                          @RequestParam(value = "page", defaultValue = "1") int page) {
        return ResponseEntity.ok(userService.findByStatus(status, pageSize, page));
    }

    @GetMapping("/find")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> findBy(@RequestParam(value = "username", required = false) String usernamePrefix,
                                    @RequestParam(value = "email", required = false) String emailPart,
                                    @RequestParam(value = "roles", required = false) List<UserRole> roles,
                                    @RequestParam(value = "statuses", required = false) List<UserStatus> statuses,
                                    @RequestParam(value = "isOnline", required = false) Boolean isOnline,
                                    @RequestParam(value = "pageSize") int pageSize,
                                    @RequestParam(value = "page") int page) {
        return ResponseEntity.ok(userService.findBy(usernamePrefix, emailPart, roles, statuses, isOnline, pageSize, page));
    }

    @PostMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> saveAll(@RequestBody List<UserDTO> userDTOS) {
        return ResponseEntity.ok(userService.saveAll(userDTOS));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> save(@RequestBody UserDTO regularUser) {
        return ResponseEntity.ok(userService.save(regularUser));
    }

    @PutMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> updateAll(@RequestBody List<UserDTO> userDTOS) {
        return ResponseEntity.ok(userService.updateAll(userDTOS));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    public ResponseEntity<?> update(@RequestBody UserDTO regularUser) {
        return ResponseEntity.ok(userService.update(regularUser));
    }
}
