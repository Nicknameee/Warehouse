package io.store.ua.entity.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Data
@RedisHash("currencyRate")
@Immutable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRate {
    @Id
    private String currencyCode;
    private String baseCurrencyCode;
    private BigDecimal rate;
    private LocalDateTime createdAt = LocalDateTime.now(Clock.systemUTC());
    @TimeToLive
    private long expiryTime;
}
