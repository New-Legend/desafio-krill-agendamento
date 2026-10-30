package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.CadastroRecebimentoUseCase;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Cadastros do recebimento", description = "CDs, fornecedores, produtos/pesos e parâmetros de descarga")
public class CadastroRecebimentoController {

    public record FornecedorResp(Long id, String cnpj, String cnpjFormatado, String razaoSocial, String nomeFantasia,
                                 String email, String telefone, String contato, boolean ativo) {
        static FornecedorResp de(Fornecedor f) {
            return new FornecedorResp(f.id(), f.cnpj(), f.cnpjFormatado(), f.razaoSocial(), f.nomeFantasia(), f.email(),
                    f.telefone(), f.contato(), f.ativo());
        }
    }

    private final CadastroRecebimentoUseCase useCase;

    public CadastroRecebimentoController(CadastroRecebimentoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/cds")
    @Operation(summary = "Centros de distribuição (multi-CD)")
    public List<CentroDistribuicao> cds() {
        return useCase.listarCds();
    }

    @PostMapping("/cds")
    @ResponseStatus(HttpStatus.CREATED)
    public CentroDistribuicao criarCd(@Valid @RequestBody Requisicoes.CentroDistribuicao b, HttpServletRequest req) {
        return useCase.salvarCd(cd(null, b), Requisicao.solicitante(req));
    }

    @PutMapping("/cds/{id}")
    public CentroDistribuicao alterarCd(@PathVariable Long id, @Valid @RequestBody Requisicoes.CentroDistribuicao b,
                                        HttpServletRequest req) {
        return useCase.salvarCd(cd(id, b), Requisicao.solicitante(req));
    }

    @GetMapping("/fornecedores")
    @Operation(summary = "Fornecedores (busca por razão social, fantasia ou CNPJ)")
    public List<FornecedorResp> fornecedores(@RequestParam(required = false) String busca,
                                             @RequestParam(required = false) Boolean ativo) {
        return useCase.listarFornecedores(busca, ativo).stream().map(FornecedorResp::de).toList();
    }

    @GetMapping("/fornecedores/{id}")
    public FornecedorResp fornecedor(@PathVariable Long id) {
        return FornecedorResp.de(useCase.fornecedor(id));
    }

    @PostMapping("/fornecedores")
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResp criarFornecedor(@Valid @RequestBody Requisicoes.Fornecedor b, HttpServletRequest req) {
        return FornecedorResp.de(useCase.salvarFornecedor(fornecedor(null, b), Requisicao.solicitante(req)));
    }

    @PutMapping("/fornecedores/{id}")
    public FornecedorResp alterarFornecedor(@PathVariable Long id, @Valid @RequestBody Requisicoes.Fornecedor b,
                                            HttpServletRequest req) {
        return FornecedorResp.de(useCase.salvarFornecedor(fornecedor(id, b), Requisicao.solicitante(req)));
    }

    @GetMapping("/produtos")
    @Operation(summary = "Produtos recebidos; semPeso=true lista os que faltam pesar")
    public List<Produto> produtos(@RequestParam(required = false) String busca,
                                  @RequestParam(required = false) Boolean semPeso) {
        return useCase.listarProdutos(busca, semPeso);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public Produto criarProduto(@Valid @RequestBody Requisicoes.Produto b, HttpServletRequest req) {
        return useCase.salvarProduto(produto(null, b), Requisicao.solicitante(req));
    }

    @PutMapping("/produtos/{id}")
    public Produto alterarProduto(@PathVariable Long id, @Valid @RequestBody Requisicoes.Produto b, HttpServletRequest req) {
        return useCase.salvarProduto(produto(id, b), Requisicao.solicitante(req));
    }

    @PutMapping("/produtos/{id}/peso")
    @Operation(summary = "Atualiza peso bruto (kg por unidade) e itens por palete")
    public Produto peso(@PathVariable Long id, @Valid @RequestBody Requisicoes.PesoProduto b, HttpServletRequest req) {
        return useCase.atualizarPeso(id, b.pesoBrutoKg(), b.itensPorPalete(), Requisicao.solicitante(req));
    }

    @GetMapping("/parametros/descarga")
    @Operation(summary = "Tempo previsto, tolerância de espera e custo de descarga por tipo de caminhão")
    public List<ParametroDescarga> parametrosDescarga() {
        return useCase.listarParametrosDescarga();
    }

    @PutMapping("/parametros/descarga/{tipoCaminhaoId}")
    public ParametroDescarga salvarDescarga(@PathVariable Long tipoCaminhaoId, @Valid @RequestBody Requisicoes.ParametroDescarga b,
                                            HttpServletRequest req) {
        return useCase.salvarParametroDescarga(new ParametroDescarga(tipoCaminhaoId, b.minutosPrevistos(),
                b.toleranciaEsperaMinutos(), b.custoPorPalete()), Requisicao.solicitante(req));
    }

    private static CentroDistribuicao cd(Long id, Requisicoes.CentroDistribuicao b) {
        return new CentroDistribuicao(id, b.codigo(), b.nome(), b.cidade(), b.capacidadeDiariaPaletes(),
                b.capacidadeFaixaPaletes(), b.ativo() == null || b.ativo());
    }

    private static Fornecedor fornecedor(Long id, Requisicoes.Fornecedor b) {
        return new Fornecedor(id, b.cnpj(), b.razaoSocial(), b.nomeFantasia(), b.email(), b.telefone(), b.contato(),
                b.ativo() == null || b.ativo());
    }

    private static Produto produto(Long id, Requisicoes.Produto b) {
        return new Produto(id, b.codigo(), b.ean(), b.descricao(), b.unidade(), b.pesoBrutoKg(), b.itensPorPalete(),
                b.ativo() == null || b.ativo());
    }
}
