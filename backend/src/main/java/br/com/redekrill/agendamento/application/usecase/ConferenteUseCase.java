package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.ConferenteRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Conferente;

import java.util.List;
import java.util.Map;

/** Conferentes por CD. Quando o CD tem conferentes ativos, a conferência só inicia com um deles. */
public class ConferenteUseCase {

    public record Dados(Long cdId, String nome, Boolean ativo) {
    }

    private final ConferenteRepositorio repo;
    private final CentroDistribuicaoRepositorio cds;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public ConferenteUseCase(ConferenteRepositorio repo, CentroDistribuicaoRepositorio cds, AuditoriaUseCase auditoria,
                             Transacao transacao) {
        this.repo = repo;
        this.cds = cds;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    public List<Conferente> listar(Long cdId, boolean somenteAtivos) {
        return repo.listar(cdId).stream().filter(c -> !somenteAtivos || c.ativo()).toList();
    }

    public Conferente salvar(Long id, Dados d, Solicitante s) {
        return transacao.executar(() -> {
            Long cd = d.cdId() == null ? 1L : d.cdId();
            cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
            if (id != null) repo.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conferente", id));
            Conferente c = new Conferente(id, cd, d.nome(), d.ativo() == null || d.ativo());
            if (repo.existeNome(cd, c.nome(), id)) throw new RegraNegocioException("Já existe um conferente com este nome neste CD");
            Conferente salvo = repo.salvar(c);
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "CONFERENTE", salvo.id(),
                    Map.of("nome", salvo.nome(), "cdId", cd, "ativo", salvo.ativo()));
            return salvo;
        });
    }

    /**
     * Valida o conferente informado ao iniciar a conferência. CD sem cadastro aceita texto livre
     * (como no desafio original); com cadastro, só nomes ativos daquele CD.
     */
    public String validarParaConferencia(Long cdId, String nome) {
        List<Conferente> ativos = listar(cdId == null ? 1L : cdId, true);
        if (ativos.isEmpty() || nome == null || nome.isBlank()) return nome;
        return ativos.stream().filter(c -> c.nome().equalsIgnoreCase(nome.trim().replaceAll("\\s+", " "))).findFirst()
                .map(Conferente::nome)
                .orElseThrow(() -> new RegraNegocioException("Conferente não cadastrado ou pausado neste CD"));
    }
}
