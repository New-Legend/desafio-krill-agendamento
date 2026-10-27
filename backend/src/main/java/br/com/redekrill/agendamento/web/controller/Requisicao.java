package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

final class Requisicao {
    private Requisicao() {
    }

    /** IP do cliente (respeita X-Forwarded-For via forward-headers-strategy do Spring). */
    static String ip(HttpServletRequest req) {
        return req.getRemoteAddr();
    }

    static Solicitante solicitante(HttpServletRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return new Solicitante(auth == null ? "anonimo" : auth.getName(), ip(req));
    }
}
