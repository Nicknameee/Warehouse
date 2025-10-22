package io.store.ua.controllers;

import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.models.dto.RegularUserDTO;
import io.store.ua.models.api.Response;
import io.store.ua.service.RegularUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final RegularUserService regularUserService;

  @GetMapping
  public Response getUser() {
    return RegularUserService.getCurrentlyAuthenticatedUser()
        .map(Response::ok)
        .orElseGet(() -> Response.of(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/find")
  @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
  public Response findUsers(
      @RequestParam(value = "username", required = false) String usernamePrefix,
      @RequestParam(value = "email", required = false) String emailPart,
      @RequestParam(value = "role", required = false) List<Role> roles,
      @RequestParam(value = "status", required = false) List<Status> statuses,
      @RequestParam(value = "isOnline", required = false) Boolean isOnline,
      @RequestParam(value = "pageSize") int pageSize,
      @RequestParam(value = "page") int page) {
    return Response.ok(
        regularUserService.findBy(
            usernamePrefix, emailPart, roles, statuses, isOnline, pageSize, page));
  }

  @GetMapping("/find/byStatus")
  @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
  public Response findUsersByStatus(
      @RequestParam("status") Status status,
      @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
      @RequestParam(value = "page", defaultValue = "1") int page) {
    return Response.ok(regularUserService.findByStatus(status, pageSize, page));
  }

  @GetMapping("/find/byRole")
  @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
  public Response findUsersByRole(
      @RequestParam("role") Role role,
      @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
      @RequestParam(value = "page", defaultValue = "1") int page) {
    return Response.ok(regularUserService.findByRole(role, pageSize, page));
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
  public Response addUser(@RequestBody RegularUserDTO regularUser) {
    return Response.ok(regularUserService.save(regularUser));
  }

  @PutMapping
  @PreAuthorize("hasAnyAuthority('MANAGER', 'DEV_OPS', 'OWNER')")
  public Response updateUser(@RequestBody RegularUserDTO regularUser) {
    return Response.ok(regularUserService.update(regularUser));
  }
}
