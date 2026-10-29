package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.TipoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.TipoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Cadastro dos tipos de palete (vale-palete → configurações). */
public class TipoPaleteUseCase {

    public record Dados(String codigo, String nome, BigDecimal valorReposicao, Integer ordem, Boolean ativo) {
    }

    private final TipoPaleteRepositorio repo;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public TipoPaleteUseCase(TipoPaleteRepositorio repo, AuditoriaUseCase auditoria, Transacao transacao) {
        this.repo = repo;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public List<TipoPalete> listar(boolean somenteAtivos) {
        return repo.listar().stream().filter(t -> !somenteAtivos || t.ativo()).toList();
    }

    public TipoPalete criar(Dados d, Solicitante s) {
        return transacao.executar(() -> {
            TipoPalete t = new TipoPalete(null, d.codigo(), d.nome(), d.valorReposicao(), ordem(d), d.ativo() == null || d.ativo());
            if (repo.existeCodigo(t.codigo(), null)) throw new RegraNegocioException("Já existe um tipo de palete com o código " + t.codigo());
            TipoPalete salvo = repo.salvar(t);
            auditoria.registrar(s, "CRIAR", "TIPO_PALETE", salvo.id(), Map.of("codigo", salvo.codigo(), "nome", salvo.nome()));
            return salvo;
        });
    }

    /** O código não muda (é a chave gravada nos vales); nome, valor, ordem e situação mudam. */
    public TipoPalete atualizar(Long id, Dados d, Solicitante s) {
        return transacao.executar(() -> {
            TipoPalete atual = buscar(id);
            if (d.codigo() != null && !d.codigo().isBlank() && !d.codigo().trim().equalsIgnoreCase(atual.codigo())) {
                throw new RegraNegocioException("O código do tipo de palete não pode ser alterado");
            }
            boolean ativo = d.ativo() == null ? atual.ativo() : d.ativo();
            if (!ativo && listar(true).stream().filter(t -> !t.id().equals(id)).findAny().isEmpty()) {
                throw new RegraNegocioException("Mantenha ao menos um tipo de palete ativo");
            }
            TipoPalete salvo = repo.salvar(new TipoPalete(id, atual.codigo(), d.nome(), d.valorReposicao(),
                    d.ordem() == null ? atual.ordem() : d.ordem(), ativo));
            auditoria.registrar(s, "ALTERAR", "TIPO_PALETE", id, Map.of("codigo", salvo.codigo(), "nome", salvo.nome(),
                    "ativo", salvo.ativo()));
            return salvo;
        });
    }

    /** Só exclui tipo nunca usado; usado em vale/romaneio deve ser inativado. */
    public void excluir(Long id, Solicitante s) {
        transacao.executar(() -> {
            TipoPalete t = buscar(id);
            if (repo.emUso(t.codigo())) {
                throw new RegraNegocioException("Tipo " + t.nome() + " já foi usado em vales ou romaneios. Inative em vez de excluir");
            }
            if (t.ativo() && listar(true).size() <= 1) throw new RegraNegocioException("Mantenha ao menos um tipo de palete ativo");
            repo.excluir(id);
            auditoria.registrar(s, "EXCLUIR", "TIPO_PALETE", id, Map.of("codigo", t.codigo()));
            return null;
        });
    }

    public TipoPalete buscar(Long id) {
        return repo.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de palete", id));
    }

    /** Novos vales e romaneios só aceitam tipos cadastrados e ativos. */
    public void validarNovo(ValePalete.Quantidades q) {
        Set<String> ativos = listar(true).stream().map(TipoPalete::codigo).collect(Collectors.toSet());
        String invalidos = q.tipos().stream().filter(c -> !ativos.contains(c)).collect(Collectors.joining(", "));
        if (!invalidos.isEmpty()) throw new RegraNegocioException("Tipo de palete inexistente ou inativo: " + invalidos);
    }

    private static int ordem(Dados d) {
        return d.ordem() == null ? 99 : d.ordem();
    }
}
