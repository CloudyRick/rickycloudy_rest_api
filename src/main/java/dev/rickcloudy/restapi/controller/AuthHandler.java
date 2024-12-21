package dev.rickcloudy.restapi.controller;

import dev.rickcloudy.restapi.dto.AuthRequest;
import dev.rickcloudy.restapi.dto.RefreshTokenRequest;
import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthHandler {
    private static Logger log = LogManager.getLogger(AuthHandler.class);
    private final AuthService authService;


    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(AuthRequest.class)
                .flatMap(loginRequest -> authService.login(loginRequest.getUsername(), loginRequest.getPassword()))
                .flatMap(authResponse -> ServerResponse.ok()
                        .body(Mono.just(ResponseDTO.success(authResponse, "Login Successful")), ResponseDTO.class));
    }

//    @CrossOrigin
    public Mono<ServerResponse> refreshToken(ServerRequest request) {
        log.debug("AuthHandler::refreshToken::reached ");
        return request.bodyToMono(RefreshTokenRequest.class)  // Use DTO to receive refresh token
                .flatMap(refreshTokenRequest -> {
                    String refreshToken = refreshTokenRequest.getRefreshToken();
                    return authService.refreshToken(refreshToken) // Pass the refresh token to service
                            .flatMap(authTokens ->
                                    ServerResponse.ok()
                                            .body(Mono.just(ResponseDTO.success(authTokens, "Refresh Token Obtained")), ResponseDTO.class)
                            );
                });
    }
}
