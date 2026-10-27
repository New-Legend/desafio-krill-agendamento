package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.NotaFiscalRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.domain.service.LeitorNfeXml;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Importação do XML da NF-e no agendamento: cria os produtos que ainda não existem, calcula o peso
 * pelos pesos cadastrados e vincula o fornecedor pelo CNPJ do emitente.
 */
public class NotaFiscalUseCase {

    public record Importacao(NotaFiscal nota, int produtosCriados, List<String> avisos) {
    }

    private final NotaFiscalRepositorio notas;
    private final AgendamentoRepositorio agendamentos;
    private final ProdutoRepositorio produtos;
    private final FornecedorRepositorio fornecedores;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;
    private final LeitorNfeXml leitor = new LeitorNfeXml();

    public NotaFiscalUseCase(NotaFiscalRepositorio notas, AgendamentoRepositorio agendamentos, ProdutoRepositorio produtos,
                             FornecedorRepositorio fornecedores, AuditoriaUseCase auditoria, Transacao transacao, Clock clock) {
        this.notas = notas;
        this.agendamentos = agendamentos;
        this.produtos = produtos;
        this.fornecedores = fornecedores;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
    }

    public Importacao importar(Long agendamentoId, byte[] xml, Solicitante s) {
        NotaFiscal lida = leitor.ler(xml);
        return transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            if (notas.existeChave(lida.chave())) throw new RegraNegocioException("NF-e " + lida.numero() + " já importada");
            List<String> avisos = new ArrayList<>();
            int criados = 0;
            List<NotaFiscal.Item> itens = new ArrayList<>();
            for (NotaFiscal.Item i : lida.itens()) {
                Optional<Produto> p = produtos.buscarPorCodigo(i.codigo());
                if (p.isEmpty() && i.ean() != null) p = produtos.buscarPorEan(i.ean());
                Produto produto = p.orElse(null);
                if (produto == null) {
                    produto = produtos.salvar(new Produto(null, i.codigo(), i.ean(), i.descricao(), i.unidade(), null, null, true));
                    criados++;
                }
                if (produto.pesoBrutoKg() == null) avisos.add("Produto " + produto.codigo() + " sem peso cadastrado");
                itens.add(i.comPeso(produto.pesoPara(i.quantidade()), produto.id()));
            }
            NotaFiscal nota = lida.comVinculo(a.getId(), itens, s.usuario(), OffsetDateTime.now(clock));
            if (nota.pesoDivergente()) {
                avisos.add("Peso da NF (" + nota.pesoBrutoKg() + " kg) difere do calculado (" + nota.pesoCalculadoKg() + " kg)");
            }
            if (nota.emitenteCnpj() != null) {
                fornecedores.buscarPorCnpj(nota.emitenteCnpj()).ifPresentOrElse(f -> {
                    if (a.getFornecedorId() == null) {
                        a.vincular(null, f.id());
                        agendamentos.salvar(a);
                    } else if (!f.id().equals(a.getFornecedorId())) {
                        avisos.add("Emitente da NF é diferente do fornecedor do agendamento");
                    }
                }, () -> avisos.add("Emitente " + nota.emitenteCnpj() + " não está no cadastro de fornecedores"));
            }
            NotaFiscal salva = notas.salvar(nota);
            auditoria.registrar(s, "IMPORTAR_NFE", "AGENDAMENTO", a.getId(), Map.of("chave", salva.chave(),
                    "numero", salva.numero(), "itens", salva.itens().size(), "avisos", avisos));
            return new Importacao(salva, criados, avisos);
        });
    }

    public List<NotaFiscal> listar(Long agendamentoId) {
        return notas.listarPorAgendamento(agendamentoId);
    }

    public void excluir(Long notaId, Solicitante s) {
        transacao.executar(() -> {
            NotaFiscal n = notas.buscarPorId(notaId).orElseThrow(() -> new RecursoNaoEncontradoException("Nota fiscal", notaId));
            Agendamento a = agendamentos.buscarPorId(n.agendamentoId()).orElseThrow();
            if (a.getFimRecebimentoEm() != null) throw new RegraNegocioException("Recebimento concluído: a NF não pode ser removida");
            notas.excluir(notaId);
            auditoria.registrar(s, "EXCLUIR_NFE", "AGENDAMENTO", n.agendamentoId(), Map.of("chave", n.chave()));
            return null;
        });
    }
}
