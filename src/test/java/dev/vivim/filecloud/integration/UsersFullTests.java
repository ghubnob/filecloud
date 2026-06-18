package dev.vivim.filecloud.integration;

import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.model.User;
import dev.vivim.filecloud.repository.UserRepository;
import dev.vivim.filecloud.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
@Transactional
public class UsersFullTests {
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;

    @Test
    void shouldCreateUserWhenRegister() throws InterruptedException {
        Assertions.assertDoesNotThrow(
                () -> userService.register(new RegisterUserRequest("username1","password1"))
        );

        Thread.sleep(3000);
        Optional<User> userOpt = userRepository.findByUsername("username1");
        Assertions.assertTrue(userOpt.isPresent());
        Assertions.assertEquals("username1", userOpt.get().getUsername());
    }
}
