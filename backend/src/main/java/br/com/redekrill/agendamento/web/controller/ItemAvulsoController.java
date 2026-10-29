package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.ItemAvulsoUseCase;
import br.com.redekrill.agendamento.domain.model.ItemAvulso;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Produtos no recebimento", description = "Produto avulso na conferência e exportação do catálogo")
public class ItemAvulsoController {
    private final ItemAvulsoUseCase useCase;

    public ItemAvulsoController(ItemAvulsoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/agendamentos/{id}/itens-avulsos")
    public List<ItemAvulso> listar(@PathVariable Long id) {
        return useCase.listar(id);
    }

    @PostMapping("/agendamentos/{id}/itens-avulsos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lança produto avulso (fora da NF-e) na conferência; pode cadastrar o produto no catálogo")
    public ItemAvulso adicionar(@PathVariable Long id, @Valid @RequestBody Requisicoes.ItemAvulso b, HttpServletRequest req) {
        return useCase.adicionar(id, new ItemAvulsoUseCase.Dados(b.produtoId(), b.codigo(), b.descricao(), b.quantidade(),
                b.unidade(), b.observacao(), Boolean.TRUE.equals(b.cadastrarProduto())), Requisicao.solicitante(req));
    }

    @GetMapping(value = "/produtos/exportar.csv", produces = "text/csv")
    @Operation(summary = "Exporta o catálogo de produtos em CSV")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String busca) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=catalogo_produtos.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(useCase.exportarCatalogoCsv(busca).getBytes(StandardCharsets.UTF_8));
    }
}
