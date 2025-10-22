package io.store.ua.events.listeners;

import io.store.ua.entity.RegularUser;
import io.store.ua.events.LogoutEvent;
import io.store.ua.repository.RegularUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("users")
public class LogoutListener implements ApplicationListener<LogoutEvent> {
  private final RegularUserRepository regularUserRepository;

  @Override
  public void onApplicationEvent(LogoutEvent event) {
    RegularUser userDetails = (RegularUser) event.getUserDetails();

    regularUserRepository.updateLogoutTime(userDetails.getId());
  }
}
