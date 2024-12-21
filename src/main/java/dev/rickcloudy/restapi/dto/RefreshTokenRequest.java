package dev.rickcloudy.restapi.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
