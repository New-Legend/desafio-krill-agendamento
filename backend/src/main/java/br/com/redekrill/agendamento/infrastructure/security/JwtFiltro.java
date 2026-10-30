package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.application.usecase.AcessoUseCase;
import br.com.redekrill.agendamento.domain.model.Permissao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida o JWT e carrega, a cada requisição, o usuário e as permissões do perfil no banco: inativar o usuário
 * ou mudar o perfil vale na hora, sem esperar o token expirar. Com troca de senha pendente só /auth responde.
 */
@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtService jwt;
    private final AcessoUseCase acessos;

    public JwtFiltro(JwtService jwt, AcessoUseCase acessos) {
        this.jwt = jwt;
        this.acessos = acessos;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.validar(header.substring(7));
                var ctx = acessos.contextoAtual(c.getSubject()).orElse(null);
                if (ctx != null) {
                    if (ctx.usuario().trocarSenha() && !req.getRequestURI().startsWith("/api/v1/auth/")) {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write("{\"status\":403,\"mensagem\":\"Troque a senha provisória para continuar\",\"detalhes\":[]}");
                        return;
                    }
                    var authorities = ctx.permissoes().stream().map(p -> new SimpleGrantedAuthority(Permissao.porCodigo(p)
                            .map(Permissao::authority).orElse("PERM_" + p))).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(ctx.usuario().login(), null, authorities));
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
