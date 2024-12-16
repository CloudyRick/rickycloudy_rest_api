    package dev.rickcloudy.restapi.security;

    import dev.rickcloudy.restapi.service.CustomUserDetailService;
    import dev.rickcloudy.restapi.utils.JwtUtils;
    import lombok.RequiredArgsConstructor;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import org.springframework.security.authentication.BadCredentialsException;
    import org.springframework.security.authentication.ReactiveAuthenticationManager;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.AuthenticationException;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Component;
    import reactor.core.publisher.Mono;

    @Component
    @RequiredArgsConstructor
    public class CustomReactiveAuthenticationManager implements ReactiveAuthenticationManager {
        private static final Logger log = LogManager.getLogger(CustomReactiveAuthenticationManager.class);
        private final JwtUtils jwtUtils;

        @Override
        public Mono<Authentication> authenticate(Authentication authentication) throws AuthenticationException {
            log.debug("Custom Reactive Auth Manager::authenticate::reached ");
            String token = authentication.getCredentials().toString();

            if (jwtUtils.validateAccessToken(token)) {
                String username = jwtUtils.extractUsernameFromAccessToken(token);

                // You can add roles/authorities based on your JWT claims if needed
                return Mono.just(new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        null// Example authority
                ));
            }
            return Mono.empty(); // Authentication failed
        }
    }
