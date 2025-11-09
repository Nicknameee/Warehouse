package io.store.ua.models.dto;

import io.store.ua.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionResultDTO {
    private User user;
    private Boolean success;
    private Throwable error;
}
