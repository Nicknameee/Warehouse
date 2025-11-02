package io.store.ua.controllers;

import io.store.ua.models.dto.LoginDTO;
import io.store.ua.models.dto.LoginResponseDTO;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.UserSecurityStrategyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String token =
                authenticationService.authenticate(loginDTO.getLogin(), loginDTO.getPassword(), request);

        return ResponseEntity.ok(
                LoginResponseDTO.builder()
                        .token(token)
                        .expirationDateMs(
                                BigInteger.valueOf(
                                        authenticationService.getExpirationDateFromToken(token).getTime()))
                        .authenticationType(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE)
                        .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authenticationService.blacklistToken(
                authorization.replace(
                        "%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE), ""));

        return ResponseEntity.ok().build();
    }
}
