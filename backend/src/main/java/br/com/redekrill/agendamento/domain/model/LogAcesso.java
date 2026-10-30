package br.com.redekrill.agendamento.domain.model;

import java.time.Instant;

public record LogAcesso(Long id, String usuario, EventoAcesso evento, String ip, String navegador, Instant criadoEm) {

    /** "Chrome · Windows" a partir do User-Agent (mesma leitura do Gestão CD). */
    public static String resumoNavegador(String ua) {
        String s = ua == null ? "" : ua;
        String so = s.contains("Android") ? "Android" : s.matches(".*(iPhone|iPad).*") ? "iOS" : s.contains("Windows") ? "Windows"
                : s.contains("Mac OS") ? "macOS" : s.contains("Linux") ? "Linux" : "";
        String nav = s.contains("Edg/") ? "Edge" : s.contains("OPR/") ? "Opera" : s.contains("Chrome/") ? "Chrome"
                : s.contains("Firefox/") ? "Firefox" : s.contains("Safari/") ? "Safari" : s.isEmpty() ? "" : "Outro";
        String r = String.join(" · ", java.util.stream.Stream.of(nav, so).filter(x -> !x.isEmpty()).toList());
        return r.isEmpty() ? "—" : r;
    }
}
