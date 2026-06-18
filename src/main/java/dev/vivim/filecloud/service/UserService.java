package dev.vivim.filecloud.service;

import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.dto.response.UserResponse;
import dev.vivim.filecloud.events.UserRegisteredEvent;
import dev.vivim.filecloud.exception.UserExistsException;
import dev.vivim.filecloud.model.User;
import dev.vivim.filecloud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent())
            throw new UserExistsException("Username is already in use!");
        User saved = userRepository.save(User.createOf(request.username(), passwordEncoder.encode(request.password())));
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
        return new UserResponse(request.username());
    }
}
