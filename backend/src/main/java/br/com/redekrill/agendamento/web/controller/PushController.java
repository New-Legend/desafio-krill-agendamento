package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.infrastructure.push.ChavesVapid;
import br.com.redekrill.agendamento.infrastructure.push.NotificadorWebPush;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@Tag(name = "Notificações push")
public class PushController {

    private final ChavesVapid chaves;
    private final NotificadorWebPush push;

    public PushController(ChavesVapid chaves, NotificadorWebPush push) {
        this.chaves = chaves;
        this.push = push;
    }

    @GetMapping("/chave-publica")
    public Map<String, String> chavePublica() {
        return Map.of("chavePublica", chaves.publica());
    }

    @PostMapping("/inscricoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inscrever(@Valid @RequestBody Requisicoes.InscricaoPush b, HttpServletRequest req) {
        push.inscrever(Requisicao.solicitante(req).usuario(), b.endpoint(), b.p256dh(), b.auth());
    }

    @DeleteMapping("/inscricoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@RequestParam String endpoint) {
        push.remover(endpoint);
    }

    @PostMapping("/teste")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void testar(HttpServletRequest req) {
        push.notificarTodos("Notificações ativas", "Este navegador receberá avisos de agendamentos.", "/painel");
    }
}
