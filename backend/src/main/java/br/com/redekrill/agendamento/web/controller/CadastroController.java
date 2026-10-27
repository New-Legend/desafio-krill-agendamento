package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.CadastroUseCase;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.FaixaResp;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cadastros")
public class CadastroController {

    private final CadastroUseCase useCase;

    public CadastroController(CadastroUseCase useCase) {
        this.useCase = useCase;
    }

    // Capacidade diária / parâmetros
    @GetMapping("/parametros/capacidade")
    public ParametroCapacidade parametro() {
        return useCase.obterParametro();
    }

    @PutMapping("/parametros/capacidade")
    @PreAuthorize("hasRole('ADMIN')")
    public ParametroCapacidade atualizarParametro(@Valid @RequestBody Requisicoes.Parametro b, HttpServletRequest req) {
        return useCase.atualizarParametro(new ParametroCapacidade(b.capacidadeDiariaPaletes(), b.quantidadeFaixas(),
                b.duracaoFaixaMinutos()), Requisicao.solicitante(req));
    }

    // Faixas horárias
    @GetMapping("/faixas-horarias")
    public List<FaixaResp> faixas() {
        return useCase.listarFaixas().stream().map(FaixaResp::de).toList();
    }

    @PostMapping("/faixas-horarias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public FaixaResp criarFaixa(@Valid @RequestBody Requisicoes.Faixa b, HttpServletRequest req) {
        return FaixaResp.de(useCase.salvarFaixa(faixa(null, b), Requisicao.solicitante(req)));
    }

    @PutMapping("/faixas-horarias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FaixaResp atualizarFaixa(@PathVariable Long id, @Valid @RequestBody Requisicoes.Faixa b, HttpServletRequest req) {
        return FaixaResp.de(useCase.salvarFaixa(faixa(id, b), Requisicao.solicitante(req)));
    }

    private static FaixaHoraria faixa(Long id, Requisicoes.Faixa b) {
        return new FaixaHoraria(id, b.horaInicio(), b.horaFim(), b.capacidadePaletes(), b.ativo() == null || b.ativo());
    }

    // Tipos de caminhão
    @GetMapping("/tipos-caminhao")
    public List<TipoCaminhao> tiposCaminhao() {
        return useCase.listarTiposCaminhao();
    }

    @PostMapping("/tipos-caminhao")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoCaminhao criarTipoCaminhao(@Valid @RequestBody Requisicoes.TipoCaminhao b, HttpServletRequest req) {
        return useCase.salvarTipoCaminhao(caminhao(null, b), Requisicao.solicitante(req));
    }

    @PutMapping("/tipos-caminhao/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TipoCaminhao atualizarTipoCaminhao(@PathVariable Long id, @Valid @RequestBody Requisicoes.TipoCaminhao b,
                                              HttpServletRequest req) {
        return useCase.salvarTipoCaminhao(caminhao(id, b), Requisicao.solicitante(req));
    }

    private static TipoCaminhao caminhao(Long id, Requisicoes.TipoCaminhao b) {
        return new TipoCaminhao(id, b.nome(), b.descricao(), b.capacidadePaletes(), b.ativo() == null || b.ativo());
    }

    // Tipos de paletização
    @GetMapping("/tipos-paletizacao")
    public List<TipoPaletizacao> tiposPaletizacao() {
        return useCase.listarTiposPaletizacao();
    }

    @PostMapping("/tipos-paletizacao")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoPaletizacao criarTipoPaletizacao(@Valid @RequestBody Requisicoes.TipoPaletizacao b, HttpServletRequest req) {
        return useCase.salvarTipoPaletizacao(paletizacao(null, b), Requisicao.solicitante(req));
    }

    @PutMapping("/tipos-paletizacao/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TipoPaletizacao atualizarTipoPaletizacao(@PathVariable Long id, @Valid @RequestBody Requisicoes.TipoPaletizacao b,
                                                    HttpServletRequest req) {
        return useCase.salvarTipoPaletizacao(paletizacao(id, b), Requisicao.solicitante(req));
    }

    private static TipoPaletizacao paletizacao(Long id, Requisicoes.TipoPaletizacao b) {
        return new TipoPaletizacao(id, b.nome(), b.fatorMultiplicador(), b.ativo() == null || b.ativo());
    }
}
