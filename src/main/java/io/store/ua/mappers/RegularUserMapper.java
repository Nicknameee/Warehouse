package io.store.ua.mappers;

import io.store.ua.entity.RegularUser;
import io.store.ua.models.dto.RegularUserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegularUserMapper {
    RegularUser toRegularUser(RegularUserDTO regularUserDTO);
}
