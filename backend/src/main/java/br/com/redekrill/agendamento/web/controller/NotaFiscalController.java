package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.NotaFiscalUseCase;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "NF-e do recebimento", description = "Importação do XML da NF-e, itens e pesos")
public class NotaFiscalController {

    public record NotaResp(Long id, Long agendamentoId, String chave, String numero, String serie, String emitenteCnpj,
                           String emitenteNome, OffsetDateTime emissao, BigDecimal valorTotal, BigDecimal pesoBrutoKg,
                           BigDecimal pesoCalculadoKg, boolean pesoDivergente, long itensSemPeso, Integer volumes,
                           List<NotaFiscal.Item> itens, String importadoPor, OffsetDateTime importadoEm) {
        static NotaResp de(NotaFiscal n) {
            return new NotaResp(n.id(), n.agendamentoId(), n.chave(), n.numero(), n.serie(), n.emitenteCnpj(),
                    n.emitenteNome(), n.emissao(), n.valorTotal(), n.pesoBrutoKg(), n.pesoCalculadoKg(), n.pesoDivergente(),
                    n.itensSemPeso(), n.volumes(), n.itens(), n.importadoPor(), n.importadoEm());
        }
    }

    public record ImportacaoResp(NotaResp nota, int produtosCriados, List<String> avisos) {
    }

    private final NotaFiscalUseCase useCase;

    public NotaFiscalController(NotaFiscalUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(value = "/agendamentos/{id}/notas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa o XML da NF-e (até 2 MB) no agendamento")
    public ImportacaoResp importar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo,
                                   HttpServletRequest req) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) throw new RegraNegocioException("Envie o arquivo XML da NF-e");
        NotaFiscalUseCase.Importacao r = useCase.importar(id, arquivo.getBytes(), Requisicao.solicitante(req));
        return new ImportacaoResp(NotaResp.de(r.nota()), r.produtosCriados(), r.avisos());
    }

    @GetMapping("/agendamentos/{id}/notas")
    public List<NotaResp> listar(@PathVariable Long id) {
        return useCase.listar(id).stream().map(NotaResp::de).toList();
    }

    @DeleteMapping("/notas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(@PathVariable Long id, HttpServletRequest req) {
        useCase.excluir(id, Requisicao.solicitante(req));
    }
}
