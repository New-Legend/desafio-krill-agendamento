package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.dto.FiltroAuditoria;
import br.com.redekrill.agendamento.application.dto.Pagina;
import br.com.redekrill.agendamento.application.dto.RegistroAuditoria;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoria")
public class AuditoriaController {

    private final AuditoriaUseCase useCase;

    public AuditoriaController(AuditoriaUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public Pagina<RegistroAuditoria> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String entidade,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return useCase.listar(new FiltroAuditoria(inicio, fim, usuario, acao, entidade), pagina, tamanho);
    }
}
