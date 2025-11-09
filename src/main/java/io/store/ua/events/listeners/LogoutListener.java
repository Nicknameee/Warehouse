package io.store.ua.events.listeners;

import io.store.ua.entity.User;
import io.store.ua.events.LogoutEvent;
import io.store.ua.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutListener implements ApplicationListener<LogoutEvent> {
    private final UserRepository userRepository;

    @Override
    public void onApplicationEvent(LogoutEvent event) {
        User userDetails = (User) event.getUserDetails();

        userRepository.updateLogoutTime(userDetails.getId());
    }
}
