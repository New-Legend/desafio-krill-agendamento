package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.TaxaDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.CalculadoraTaxaDescarga;
import br.com.redekrill.agendamento.domain.model.TaxaDescarga;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cadastro e cálculo da taxa de descarga por modal (sider, baú, granel e outros). */
public class TaxaDescargaUseCase {

    public record Dados(String modal, String nome, BigDecimal valorPorPalete, Boolean ativo) {
    }

    private final TaxaDescargaRepositorio repo;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public TaxaDescargaUseCase(TaxaDescargaRepositorio repo, AuditoriaUseCase auditoria, Transacao transacao) {
        this.repo = repo;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public List<TaxaDescarga> listar() {
        return repo.listar();
    }

    public TaxaDescarga salvar(Long id, Dados d, Solicitante s) {
        return transacao.executar(() -> {
            String modal = d.modal();
            if (id != null) {
                TaxaDescarga atual = repo.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Taxa de descarga", id));
                if (modal != null && !modal.isBlank() && !CalculadoraTaxaDescarga.codigo(modal).equals(atual.modal())) {
                    throw new RegraNegocioException("O código do modal não pode ser alterado");
                }
                modal = atual.modal();
            }
            TaxaDescarga t = new TaxaDescarga(id, modal, d.nome(), d.valorPorPalete(), d.ativo() == null || d.ativo());
            if (repo.existeModal(t.modal(), id)) throw new RegraNegocioException("Já existe taxa para o modal " + t.modal());
            TaxaDescarga salvo = repo.salvar(t);
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "TAXA_DESCARGA", salvo.id(),
                    Map.of("modal", salvo.modal(), "valorPorPalete", salvo.valorPorPalete()));
            return salvo;
        });
    }

    public Optional<CalculadoraTaxaDescarga.Cobranca> calcular(String modal, String tipoCaminhao, int paletes) {
        return CalculadoraTaxaDescarga.calcular(repo.listar(), modal, tipoCaminhao, paletes);
    }
}
