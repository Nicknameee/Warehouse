package io.store.ua.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;

@Getter
public class LoginEvent extends ApplicationEvent {
    private final UserDetails userDetails;

    public LoginEvent(UserDetails userDetails) {
        super(userDetails, Clock.systemUTC());
        this.userDetails = userDetails;
    }
}
