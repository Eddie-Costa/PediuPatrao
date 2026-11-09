package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ErroApi;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroApi> parametroInvalido(IllegalArgumentException excecao, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, excecao.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroApi> tipoInvalido(MethodArgumentTypeMismatchException excecao,
            HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + excecao.getName() + "'.", request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroApi> filtroInvalido(BindException excecao, HttpServletRequest request) {
        String campo = excecao.getFieldError() == null ? "filtro" : excecao.getFieldError().getField();
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + campo + "'. Datas devem usar o formato AAAA-MM-DD.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApi> acessoNegado(AccessDeniedException excecao, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para acessar o dashboard.", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroApi> bancoIndisponivel(DataAccessException excecao, HttpServletRequest request) {
        log.error("Falha ao consultar os dados do dashboard", excecao);
        return resposta(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível consultar os dados no momento. Tente novamente em instantes.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> erroInesperado(Exception excecao, HttpServletRequest request) {
        log.error("Erro inesperado no dashboard", excecao);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno ao processar a consulta.", request);
    }

    private ResponseEntity<ErroApi> resposta(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErroApi.de(status, mensagem, request.getRequestURI()));
    }
}
