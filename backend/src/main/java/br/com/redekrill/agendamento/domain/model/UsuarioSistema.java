package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.Instant;
import java.util.regex.Pattern;

/** Usuário interno (fornecedor não tem login: usa o link público do agendamento). */
public record UsuarioSistema(Long id, String login, String nome, Long perfilId, String perfilNome, boolean ativo,
                             boolean trocarSenha, Instant ultimoAcesso, Instant criadoEm, String senhaHash) {
    private static final Pattern LOGIN = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,59}$");

    public UsuarioSistema {
        login = Textos.obrigatorio(login, "Login", 60).toLowerCase();
        if (!LOGIN.matcher(login).matches())
            throw new RegraNegocioException("Login deve ter de 3 a 60 caracteres: letras minúsculas, números, ponto, hífen ou sublinhado");
        nome = Textos.obrigatorio(nome, "Nome", 120).replaceAll("\\s+", " ");
        if (perfilId == null) throw new RegraNegocioException("Perfil é obrigatório");
    }

    /** Regra de senha: mínimo 8 caracteres, com letra e número, diferente do login. */
    public static void validarSenha(String senha, String login) {
        if (senha == null || senha.length() < 8) throw new RegraNegocioException("A senha deve ter pelo menos 8 caracteres");
        if (senha.length() > 72) throw new RegraNegocioException("A senha deve ter no máximo 72 caracteres");
        if (!senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*"))
            throw new RegraNegocioException("A senha deve ter letras e números");
        if (login != null && senha.equalsIgnoreCase(login)) throw new RegraNegocioException("A senha não pode ser igual ao login");
    }
}
