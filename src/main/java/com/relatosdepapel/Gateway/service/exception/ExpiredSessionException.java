package com.relatosdepapel.Gateway.service.exception;

import lombok.Getter;

@Getter
public class ExpiredSessionException extends Exception {
    private final String refreshToken;

    public ExpiredSessionException(String refreshToken) {
        super("Session expired");
        this.refreshToken = refreshToken;
    }

}
