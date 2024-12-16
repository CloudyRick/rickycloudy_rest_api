package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;


@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new User(user.getUsername(), user.getPassword(), new ArrayList<>()));
    }
}
