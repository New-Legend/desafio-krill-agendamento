package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.ItemAvulsoRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.ItemAvulso;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.service.CatalogoProdutoCsv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Produto avulso no recebimento: item que chegou fora da NF-e. Pode apontar para um produto do catálogo,
 * reaproveitar pelo código/EAN ou cadastrar um produto novo. Também exporta o catálogo em CSV.
 */
public class ItemAvulsoUseCase {

    public record Dados(Long produtoId, String codigo, String descricao, BigDecimal quantidade, String unidade,
                        String observacao, boolean cadastrarProduto) {
    }

    private final ItemAvulsoRepositorio repo;
    private final ProdutoRepositorio produtos;
    private final AgendamentoRepositorio agendamentos;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;

    public ItemAvulsoUseCase(ItemAvulsoRepositorio repo, ProdutoRepositorio produtos, AgendamentoRepositorio agendamentos,
                             AuditoriaUseCase auditoria, Transacao transacao, Clock clock) {
        this.repo = repo;
        this.produtos = produtos;
        this.agendamentos = agendamentos;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
    }

    public List<ItemAvulso> listar(Long agendamentoId) {
        return repo.listar(agendamentoId);
    }

    public ItemAvulso adicionar(Long agendamentoId, Dados d, Solicitante s) {
        return transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            if (a.getStatus() != StatusAgendamento.CHEGOU && a.getStatus() != StatusAgendamento.EM_RECEBIMENTO
                    && a.getStatus() != StatusAgendamento.RECEBIDO) {
                throw new RegraNegocioException("Produto avulso só pode ser lançado depois da chegada do caminhão");
            }
            Produto p = localizar(d).orElse(null);
            if (p == null && d.cadastrarProduto()) {
                p = produtos.salvar(new Produto(null, d.codigo(), null, d.descricao(), d.unidade(), null, null, true));
            }
            String codigo = p == null ? d.codigo() : p.codigo();
            String descricao = p == null ? d.descricao() : p.descricao();
            String unidade = d.unidade() == null || d.unidade().isBlank() ? (p == null ? null : p.unidade()) : d.unidade();
            ItemAvulso salvo = repo.salvar(new ItemAvulso(null, agendamentoId, p == null ? null : p.id(), codigo, descricao,
                    d.quantidade(), unidade, d.observacao(), s.usuario(), OffsetDateTime.now(clock)));
            Map<String, Object> det = new HashMap<>();
            det.put("codigo", salvo.codigo());
            det.put("quantidade", salvo.quantidade());
            det.put("produtoCadastrado", p != null);
            auditoria.registrar(s, "ITEM_AVULSO", "AGENDAMENTO", agendamentoId, det);
            return salvo;
        });
    }

    private Optional<Produto> localizar(Dados d) {
        if (d.produtoId() != null) {
            return Optional.of(produtos.buscarPorId(d.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", d.produtoId())));
        }
        if (d.codigo() == null || d.codigo().isBlank()) return Optional.empty();
        String c = d.codigo().trim();
        return produtos.buscarPorCodigo(c).or(() -> c.matches("\\d{8,14}") ? produtos.buscarPorEan(c) : Optional.empty());
    }

    public String exportarCatalogoCsv(String busca) {
        return CatalogoProdutoCsv.gerar(produtos.listar(busca, null));
    }
}
