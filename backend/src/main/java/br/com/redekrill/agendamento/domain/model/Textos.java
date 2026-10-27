package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.util.regex.Pattern;

/** Validações de texto compartilhadas pelos cadastros. */
public final class Textos {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Textos() {
    }

    public static String obrigatorio(String v, String campo, int max) {
        if (v == null || v.isBlank()) throw new RegraNegocioException(campo + " é obrigatório");
        return limite(v.trim(), campo, max);
    }

    public static String opcional(String v, String campo, int max) {
        return v == null || v.isBlank() ? null : limite(v.trim(), campo, max);
    }

    public static String email(String v, String campo, boolean obrigatorio) {
        String e = obrigatorio ? obrigatorio(v, campo, 80) : opcional(v, campo, 80);
        if (e != null && !EMAIL.matcher(e).matches()) throw new RegraNegocioException(campo + " inválido");
        return e == null ? null : e.toLowerCase();
    }

    public static String digitos(String v) {
        return v == null ? null : v.replaceAll("\\D", "");
    }

    private static String limite(String v, String campo, int max) {
        if (v.length() > max) throw new RegraNegocioException(campo + " deve ter no máximo " + max + " caracteres");
        return v;
    }
}
