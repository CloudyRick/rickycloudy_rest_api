package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.AuthResponse;
import dev.rickcloudy.restapi.exception.custom.AuthenticationException;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
import dev.rickcloudy.restapi.repository.UserRepository;
import dev.rickcloudy.restapi.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public Mono<AuthResponse> login(String username, String password) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.BAD_REQUEST,
                        "User with username " + username + " not " +
                        "found")))
                .flatMap(user -> {
                    // Verify the password
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        // Generate JWT token
                        String token = jwtUtils.generateAccessToken(username);
                        String refreshToken = jwtUtils.generateRefreshToken(username);

                        // Prepare the response
                        AuthResponse response = new AuthResponse(token, refreshToken);
                        return Mono.just(response);
                    } else {
                        return Mono.error(new AuthenticationException("Invalid username or " +
                                "password"));
                    }
                });
    }

    public Mono<AuthResponse> refreshToken(String refreshToken) {
        // Validate the refresh token
        if (jwtUtils.validateRefreshToken(refreshToken)) {
            // Extract user email (or subject) from the refresh token
            String email = jwtUtils.extractUsernameFromRefreshToken(refreshToken);

            // Generate a new access token
            String newAccessToken = jwtUtils.generateAccessToken(email);

            // Generate a new refresh token
            String newRefreshToken = jwtUtils.generateRefreshToken(email);

            // Return both the new access token and refresh token
            return Mono.just(new AuthResponse(newAccessToken, newRefreshToken));
        }

        // If the refresh token is invalid, return an error Mono
        return Mono.error(new AuthenticationException("Invalid refresh token"));
    }

}
