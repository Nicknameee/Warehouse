package io.store.ua.models.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private BigInteger expirationDateMs;
    private String authenticationType;
}
