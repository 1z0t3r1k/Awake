package com.amiawake.amiawake.user.service;

import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.common.exception.UsernameAlreadyExistsException;
import com.amiawake.amiawake.common.security.JwtService;
import com.amiawake.amiawake.user.dto.StatusResponse;
import com.amiawake.amiawake.user.dto.UserCreateRequest;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerLowercasesUsernameHashesPasswordAndSavesUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(new UserCreateRequest("Alice", "password123"));

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDisplayName()).isEqualTo("alice");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getTimeZone()).isEqualTo("UTC");
        assertThat(user.getStatus()).isEqualTo(User.AvailabilityStatus.AVAILABLE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void registerRejectsExistingUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new UserCreateRequest("Alice", "password123")))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserReturnsUserById() {
        UUID userId = UUID.randomUUID();
        User user = user("alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(userService.getUser(userId)).isSameAs(user);
    }

    @Test
    void getUserThrowsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changeStatusUpdatesUserAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        User user = user("alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        StatusResponse response = userService.changeStatus(userId, User.AvailabilityStatus.TEXT_ONLY);

        assertThat(response.status()).isEqualTo(User.AvailabilityStatus.TEXT_ONLY);
        assertThat(user.getStatus()).isEqualTo(User.AvailabilityStatus.TEXT_ONLY);
    }

    @Test
    void getStatusReturnsCurrentStatus() {
        UUID userId = UUID.randomUUID();
        User user = user("alice");
        user.changeStatus(User.AvailabilityStatus.DO_NOT_DISTURB);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        StatusResponse response = userService.getStatus(userId);

        assertThat(response.status()).isEqualTo(User.AvailabilityStatus.DO_NOT_DISTURB);
    }

    private static User user(String username) {
        return new User(UUID.randomUUID(), username, username, "hash", "UTC");
    }
}
