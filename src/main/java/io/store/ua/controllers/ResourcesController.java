package io.store.ua.controllers;

import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.utility.UserSecurityStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourcesController {
    @GetMapping("/vars/userRoles")
    public ResponseEntity<?> userRoles() {
        return ResponseEntity.ok(Role.values());
    }

    @GetMapping("/vars/userStatuses")
    public ResponseEntity<?> userStatuses() {
        return ResponseEntity.ok(Status.values());
    }

    @GetMapping("/vars/securityType")
    public ResponseEntity<?> securityType() {
        return ResponseEntity.ok(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
    }
}
