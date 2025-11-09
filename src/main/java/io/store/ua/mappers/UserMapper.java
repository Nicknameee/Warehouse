package io.store.ua.mappers;

import io.store.ua.entity.User;
import io.store.ua.models.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    User toUser(UserDTO userDTO);
}
