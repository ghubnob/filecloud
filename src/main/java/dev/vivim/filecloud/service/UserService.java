package dev.vivim.filecloud.service;

import dev.vivim.filecloud.dto.AuthorizationRequest;
import dev.vivim.filecloud.dto.RegisterUserRequest;
import dev.vivim.filecloud.dto.UserResponse;
import dev.vivim.filecloud.exception.AuthorizationException;
import dev.vivim.filecloud.exception.UserExistsException;
import dev.vivim.filecloud.model.User;
import dev.vivim.filecloud.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FileService fileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
    }

    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) throw new UserExistsException("Username is already in use!");
        userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));
        fileService.createRootFolderOnRegistration(request.username());
        return new UserResponse(request.username());
    }

    public UserResponse authorization(AuthorizationRequest request) {
        Optional<User> user = userRepository.findByUsername(request.username());
        if (user.isEmpty()) throw new AuthorizationException("User is not exists!");
        if (!passwordEncoder.matches(request.password(), user.get().getPassword())) throw new AuthorizationException("Wrong password!");
        return new UserResponse(request.username());
    }
}
