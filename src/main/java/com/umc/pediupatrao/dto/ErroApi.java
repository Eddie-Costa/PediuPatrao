package com.umc.pediupatrao.dto;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ErroApi(Instant timestamp, int status, String erro, String mensagem, String caminho) {

    public static ErroApi de(HttpStatus status, String mensagem, String caminho) {
        return new ErroApi(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, caminho);
    }
}
