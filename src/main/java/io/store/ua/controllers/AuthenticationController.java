package io.store.ua.controllers;

import io.store.ua.models.dto.LoginDTO;
import io.store.ua.models.dto.LoginResponseDTO;
import io.store.ua.models.api.Response;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.UserSecurityStrategyService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {
  private final AuthenticationService authenticationService;

  @PostMapping("/login")
  public Response login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
    String token =
        authenticationService.authenticate(loginDTO.getLogin(), loginDTO.getPassword(), request);

    return Response.data(
        LoginResponseDTO.builder()
            .token(token)
            .expirationDateMs(
                BigInteger.valueOf(
                    authenticationService.getExpirationDateFromToken(token).getTime()))
            .authenticationType(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE)
            .build());
  }

  @PostMapping("/logout")
  public Response logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    authenticationService.blacklistToken(
        authorization.replace(
            "%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE), ""));

    return Response.ok();
  }
}
