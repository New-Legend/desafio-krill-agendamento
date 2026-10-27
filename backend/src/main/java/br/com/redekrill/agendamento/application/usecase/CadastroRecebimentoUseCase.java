package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import br.com.redekrill.agendamento.domain.model.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Cadastros do recebimento: CDs, fornecedores, produtos/pesos e parâmetros de descarga. */
public class CadastroRecebimentoUseCase {

    private final CentroDistribuicaoRepositorio cds;
    private final FornecedorRepositorio fornecedores;
    private final ProdutoRepositorio produtos;
    private final ParametroDescargaRepositorio descarga;
    private final TipoCaminhaoRepositorio caminhoes;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;

    public CadastroRecebimentoUseCase(CentroDistribuicaoRepositorio cds, FornecedorRepositorio fornecedores,
                                      ProdutoRepositorio produtos, ParametroDescargaRepositorio descarga,
                                      TipoCaminhaoRepositorio caminhoes, AuditoriaUseCase auditoria, Transacao transacao) {
        this.cds = cds;
        this.fornecedores = fornecedores;
        this.produtos = produtos;
        this.descarga = descarga;
        this.caminhoes = caminhoes;
        this.auditoria = auditoria;
        this.transacao = transacao;
    }

    // ---- CDs
    public List<CentroDistribuicao> listarCds() {
        return cds.listar();
    }

    public CentroDistribuicao salvarCd(CentroDistribuicao cd, Solicitante s) {
        return transacao.executar(() -> {
            if (cd.id() != null) cds.buscarPorId(cd.id()).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd.id()));
            if (cds.existeCodigo(cd.codigo(), cd.id())) throw new RegraNegocioException("Já existe CD com o código " + cd.codigo());
            if (cd.id() != null && cd.id() == 1L && !cd.ativo()) throw new RegraNegocioException("O CD padrão não pode ser inativado");
            CentroDistribuicao salvo = cds.salvar(cd);
            auditoria.registrar(s, cd.id() == null ? "CRIAR" : "ALTERAR", "CD", salvo.id(), Map.of("codigo", salvo.codigo()));
            return salvo;
        });
    }

    // ---- Fornecedores
    public List<Fornecedor> listarFornecedores(String busca, Boolean ativo) {
        return fornecedores.listar(busca, ativo);
    }

    public Fornecedor fornecedor(Long id) {
        return fornecedores.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }

    public Fornecedor salvarFornecedor(Fornecedor f, Solicitante s) {
        return transacao.executar(() -> {
            if (f.id() != null) fornecedor(f.id());
            fornecedores.buscarPorCnpj(f.cnpj()).filter(o -> !Objects.equals(o.id(), f.id())).ifPresent(o -> {
                throw new RegraNegocioException("CNPJ já cadastrado para " + o.razaoSocial());
            });
            Fornecedor salvo = fornecedores.salvar(f);
            auditoria.registrar(s, f.id() == null ? "CRIAR" : "ALTERAR", "FORNECEDOR", salvo.id(),
                    Map.of("cnpj", salvo.cnpj(), "razaoSocial", salvo.razaoSocial()));
            return salvo;
        });
    }

    // ---- Produtos e pesos
    public List<Produto> listarProdutos(String busca, Boolean semPeso) {
        return produtos.listar(busca, semPeso);
    }

    public Produto salvarProduto(Produto p, Solicitante s) {
        return transacao.executar(() -> {
            if (p.id() != null) produtos.buscarPorId(p.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", p.id()));
            produtos.buscarPorCodigo(p.codigo()).filter(o -> !Objects.equals(o.id(), p.id())).ifPresent(o -> {
                throw new RegraNegocioException("Código de produto já cadastrado");
            });
            Produto salvo = produtos.salvar(p);
            auditoria.registrar(s, p.id() == null ? "CRIAR" : "ALTERAR", "PRODUTO", salvo.id(),
                    Map.of("codigo", salvo.codigo(), "peso", String.valueOf(salvo.pesoBrutoKg())));
            return salvo;
        });
    }

    public Produto atualizarPeso(Long id, BigDecimal peso, Integer porPalete, Solicitante s) {
        return transacao.executar(() -> {
            Produto p = produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
            Produto salvo = produtos.salvar(p.comPeso(peso, porPalete));
            auditoria.registrar(s, "PESO", "PRODUTO", id, Map.of("de", String.valueOf(p.pesoBrutoKg()),
                    "para", String.valueOf(peso)));
            return salvo;
        });
    }

    // ---- Parâmetros de descarga
    public List<ParametroDescarga> listarParametrosDescarga() {
        return descarga.listar();
    }

    public ParametroDescarga salvarParametroDescarga(ParametroDescarga p, Solicitante s) {
        return transacao.executar(() -> {
            caminhoes.buscarPorId(p.tipoCaminhaoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de caminhão", p.tipoCaminhaoId()));
            ParametroDescarga salvo = descarga.salvar(p);
            auditoria.registrar(s, "ALTERAR", "PARAMETRO_DESCARGA", p.tipoCaminhaoId(), Map.of(
                    "minutos", p.minutosPrevistos(), "tolerancia", p.toleranciaEsperaMinutos(), "custo", p.custoPorPalete().toPlainString()));
            return salvo;
        });
    }
}
