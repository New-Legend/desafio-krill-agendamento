package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * API stateless com JWT. Cada requisição recebe como authorities as permissões numeradas do perfil
 * do usuário (PERM_1103...), lidas do banco; a tabela rota → permissão fica em {@link RegrasAcesso}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtFiltro jwtFiltro) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(HttpStatus.FORBIDDEN.value())))
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:")))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .requestMatchers("/error", "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/push/chave-publica").permitAll()
                            .requestMatchers("/api/v1/publico/**").permitAll();
                    RegrasAcesso.REGRAS.stream().filter(r -> r.metodo() == HttpMethod.GET)
                            .forEach(r -> a.requestMatchers(HttpMethod.GET, r.caminho()).hasAnyAuthority(r.authorities()));
                    a.requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
                            .requestMatchers("/api/v1/auth/**", "/api/v1/push/**", "/api/v1/agendamentos/simulacao").authenticated();
                    RegrasAcesso.REGRAS.stream().filter(r -> r.metodo() != HttpMethod.GET).forEach(r -> {
                        if (r.metodo() == null) a.requestMatchers(r.caminho()).hasAnyAuthority(r.authorities());
                        else a.requestMatchers(r.metodo(), r.caminho()).hasAnyAuthority(r.authorities());
                    });
                    a.anyRequest().denyAll();
                })
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.corsOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
