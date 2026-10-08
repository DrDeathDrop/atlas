package io.github.drdeathdrop.atlas.user.account;

import io.github.drdeathdrop.atlas.user.Role;
import io.github.drdeathdrop.atlas.user.UserSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private UserService userService;

    @Test
    void createUserSavesLowercasedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("ivcho@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserSummary result = userService.createUser(
                "Ivcho@Example.com", "secret123", "Ivaylo Atanasov", Role.DISPATCHER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("ivcho@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-value");
        assertThat(saved.isEnabled()).isTrue();

        assertThat(result.email()).isEqualTo("ivcho@example.com");
        assertThat(result.role()).isEqualTo(Role.DISPATCHER);
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ivcho@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                "Ivcho@Example.com", "secret123", "Ivaylo Atanasov", Role.DISPATCHER))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("ivcho@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
