package io.store.ua.events.listeners;

import io.store.ua.entity.User;
import io.store.ua.events.LoginEvent;
import io.store.ua.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginListener implements ApplicationListener<LoginEvent> {
    private final UserRepository userRepository;

    @Override
    public void onApplicationEvent(LoginEvent event) {
        User userDetails = (User) event.getUserDetails();

        userRepository.updateLoginTime(userDetails.getId());
    }
}
