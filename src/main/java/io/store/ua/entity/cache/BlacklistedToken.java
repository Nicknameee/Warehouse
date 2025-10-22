package io.store.ua.entity.cache;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Data
@RedisHash("blacklistedToken")
public class BlacklistedToken {
    @Id
    private String tokenId;
    @TimeToLive
    private long expiryTime;
}
