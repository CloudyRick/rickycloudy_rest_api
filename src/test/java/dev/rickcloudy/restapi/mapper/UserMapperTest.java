package dev.rickcloudy.restapi.mapper;

import dev.rickcloudy.restapi.dto.UserDTO;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserMapperTest {
    @Autowired
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName(value = "User to UserDTO")
    void userToDto() {
        Users userSample = Users.builder()
                .id(1L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("ricky@gmail.com")
                .status(UserStatus.ACTIVE)
                .password("anjing")
                .build();

        UserDTO dto = userMapper.userToDto(userSample);

        assertEquals(userSample.getFirstName(), dto.getFirstName());
        assertEquals(userSample.getLastName(), dto.getLastName());
        assertEquals(userSample.getEmail(), dto.getEmail());
        assertEquals(userSample.getStatus(), dto.getStatus());
        assertEquals(userSample.getId(), dto.getId());
    }

    @Test
    @DisplayName(value = "UserDTO to User")
    void dtoToUser() {
        UserDTO dtoSample = UserDTO.builder()
                .id(2L)
                .firstName("Syahna")
                .lastName("Indira")
                .email("syahna@gmail.com")
                .status(UserStatus.ACTIVE)
                .username("syahnaindira")
                .build();

        Users user = userMapper.dtoToUser(dtoSample);

        assertEquals(user.getFirstName(), dtoSample.getFirstName());
        assertEquals(user.getLastName(), dtoSample.getLastName());
        assertEquals(user.getEmail(), dtoSample.getEmail());
        assertEquals(user.getStatus(), dtoSample.getStatus());
        assertEquals(user.getId(), dtoSample.getId());
    }
}