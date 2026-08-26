package com.amiawake.amiawake.user.service;

import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.common.exception.UsernameAlreadyExistsException;
import com.amiawake.amiawake.common.security.JwtService;
import com.amiawake.amiawake.user.dto.StatusResponse;
import com.amiawake.amiawake.user.dto.TimeZoneRequest;
import com.amiawake.amiawake.user.dto.UserCreateRequest;
import com.amiawake.amiawake.user.entity.AvailabilityStatus;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User getUserById(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new UserNotFoundException(uuid));
    }

    @Transactional
    public User register(UserCreateRequest userCreateRequest) {
        String username = userCreateRequest.username()
                .toLowerCase(Locale.ROOT);
        String password = userCreateRequest.password();

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        String passwordHash = passwordEncoder.encode(password);

        User user = new User(UUID.randomUUID(), username, username, passwordHash, "UTC");

        return userRepository.save(user);
    }

    @Transactional
    public StatusResponse changeStatus(UUID id, AvailabilityStatus status) {
        User user = getUserById(id);

        user.changeStatus(status);

        return new StatusResponse(user.getStatus());
    }

    public StatusResponse getStatus(UUID id) {
        User user = getUserById(id);

        return new StatusResponse(user.getStatus());
    }

    @Transactional
    public void changeTimeZone(UUID userId, TimeZoneRequest request) {
        User user = getUserById(userId);

        user.changeTimeZone(request.zoneId());
    }
}
