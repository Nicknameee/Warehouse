package io.store.ua.controllers;

import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.models.api.Response;
import io.store.ua.utility.UserSecurityStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourcesController {
  @GetMapping("/vars/userRoles")
  public Response userRoles() {
    return Response.ok(Role.values());
  }

  @GetMapping("/vars/userStatuses")
  public Response userStatuses() {
    return Response.ok(Status.values());
  }

  @GetMapping("/vars/securityType")
  public Response securityType() {
    return Response.ok(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
  }
}
