package dev.vivim.filecloud.module;

import dev.vivim.filecloud.dto.RegisterUserRequest;
import dev.vivim.filecloud.exception.UserExistsException;
import dev.vivim.filecloud.model.User;
import dev.vivim.filecloud.repository.UserRepository;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsersModuleTests {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FileService fileService;
    @InjectMocks UserService userService;

    @Captor ArgumentCaptor<User> userCaptor;
    @Captor ArgumentCaptor<String> usernameCaptor;

    @Test
    void shouldCreateUserInDatabase() {
        when(userRepository.save(any())).thenReturn(new User("testusername", "testpassword"));
        when(passwordEncoder.encode(any())).thenReturn("testpassword_hashed");
        userService.register(new RegisterUserRequest("testusername", "testpassword"));

        verify(userRepository, times(1)).save(userCaptor.capture());
        Assertions.assertEquals("testpassword_hashed", userCaptor.getValue().getPassword());

        verify(fileService, times(1)).createRootFolderOnRegistration(usernameCaptor.capture());
        Assertions.assertEquals("testusername", usernameCaptor.getValue());
    }

    @Test
    void shouldThrowWhenRegisterExistingUser() {
        when(userRepository.findByUsername("testusername")).thenReturn(Optional.of(new User()));
        Assertions.assertThrows(UserExistsException.class, () ->
                userService.register(new RegisterUserRequest("testusername", "testpassword")));
    }
}
