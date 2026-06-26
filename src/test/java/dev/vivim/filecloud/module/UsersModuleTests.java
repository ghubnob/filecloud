package dev.vivim.filecloud.module;

import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.events.UserRegisteredEvent;
import dev.vivim.filecloud.exception.UserExistsException;
import dev.vivim.filecloud.model.User;
import dev.vivim.filecloud.repository.UserRepository;
import dev.vivim.filecloud.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsersModuleTests {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks UserService userService;

    @Captor ArgumentCaptor<User> userCaptor;
    @Captor ArgumentCaptor<UserRegisteredEvent> regEventCaptor;

    @Test
    void shouldCreateUserInDatabase() {
        User saved = User.createOf("testusername", "testpassword");
        ReflectionTestUtils.setField(saved, "id", 1);

        String hashedPassword = "testpassword_hashed";
        when(userRepository.findByUsername("testusername")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(saved);
        when(passwordEncoder.encode(any())).thenReturn(hashedPassword);

        userService.register(new RegisterUserRequest("testusername", "testpassword"));

        verify(userRepository, times(1)).save(userCaptor.capture());
        Assertions.assertEquals(hashedPassword, userCaptor.getValue().getPassword());

        verify(applicationEventPublisher, times(1)).publishEvent(regEventCaptor.capture());
        Assertions.assertNotNull(regEventCaptor.getValue().userId());
    }

    @Test
    void shouldThrowWhenRegisterExistingUser() {
        when(userRepository.findByUsername("testusername"))
                .thenReturn(Optional.of(User.createOf("testusername", "testpassword")));
        Assertions.assertThrows(UserExistsException.class, () ->
                userService.register(new RegisterUserRequest("testusername", "testpassword")));
    }
}
