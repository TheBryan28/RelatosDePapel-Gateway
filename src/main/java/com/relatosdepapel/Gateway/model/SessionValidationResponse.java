package com.relatosdepapel.Gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionValidationResponse {
    private String accessToken;
    private String refreshToken;
}
