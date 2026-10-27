package br.com.redekrill.agendamento.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Corpos de requisição da API (validação de entrada com Bean Validation). */
public final class Requisicoes {
    private Requisicoes() {
    }

    public record Login(@NotBlank @Schema(example = "admin") String usuario, @NotBlank @Schema(example = "admin123") String senha) {
    }

    public record CriarAgendamento(
            @NotNull(message = "Data é obrigatória") @Schema(example = "2026-11-04") LocalDate data,
            @NotNull(message = "Faixa horária é obrigatória") @Schema(example = "1") Long faixaHorariaId,
            @NotNull(message = "ID do pedido é obrigatório") @Positive(message = "ID do pedido deve ser positivo") @Schema(example = "450001") Long idPedido,
            @NotBlank(message = "Fornecedor é obrigatório") @Size(max = 80, message = "Fornecedor deve ter no máximo 80 caracteres") @Schema(example = "Distribuidora Litoral Alimentos Ltda") String fornecedor,
            @NotBlank(message = "E-mail do fornecedor é obrigatório") @Size(max = 80, message = "E-mail deve ter no máximo 80 caracteres")
            @Email(message = "E-mail do fornecedor inválido") @Schema(example = "agenda@litoralalimentos.com.br") String emailFornecedor,
            @NotNull(message = "Tipo de caminhão é obrigatório") @Schema(example = "1") Long tipoCaminhaoId,
            @NotNull(message = "Tipo de paletização é obrigatório") @Schema(example = "2") Long tipoPaletizacaoId,
            @NotNull(message = "Quantidade de paletes é obrigatória") @Min(value = 1, message = "Quantidade de paletes deve ser maior que zero")
            @Max(value = 10000, message = "Quantidade de paletes acima do permitido") @Schema(example = "22") Integer quantidadePaletes,
            @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres") String observacao,
            @Schema(description = "CD de destino (padrão 1)", example = "1") Long cdId,
            @Schema(description = "Fornecedor cadastrado (opcional)") Long fornecedorId) {
    }

    public record Reagendar(@NotNull(message = "Data é obrigatória") @Schema(example = "2026-11-05") LocalDate data,
                            @NotNull(message = "Faixa horária é obrigatória") @Schema(example = "2") Long faixaHorariaId) {
    }

    public record CentroDistribuicao(@NotBlank @Size(max = 10) @Schema(example = "807") String codigo,
                                     @NotBlank @Size(max = 80) @Schema(example = "CD Rede Krill 2") String nome,
                                     @Size(max = 80) String cidade,
                                     @Min(1) @Max(100000) @Schema(description = "Vazio = usa o parâmetro geral") Integer capacidadeDiariaPaletes,
                                     @Min(1) @Max(100000) @Schema(description = "Vazio = usa a capacidade de cada faixa") Integer capacidadeFaixaPaletes,
                                     Boolean ativo) {
    }

    public record Fornecedor(@NotBlank @Size(max = 18) @Schema(example = "11.222.333/0001-81") String cnpj,
                             @NotBlank @Size(max = 80) @Schema(example = "Distribuidora Litoral Alimentos Ltda") String razaoSocial,
                             @Size(max = 80) String nomeFantasia,
                             @NotBlank @Email @Size(max = 80) @Schema(example = "agenda@litoralalimentos.com.br") String email,
                             @Size(max = 20) String telefone, @Size(max = 80) String contato, Boolean ativo) {
    }

    public record Produto(@NotBlank @Size(max = 60) @Schema(example = "7891000100103") String codigo,
                          @Size(max = 14) String ean, @NotBlank @Size(max = 120) String descricao,
                          @Size(max = 6) String unidade,
                          @jakarta.validation.constraints.DecimalMin("0") @Schema(example = "12.5") java.math.BigDecimal pesoBrutoKg,
                          @Min(1) Integer itensPorPalete, Boolean ativo) {
    }

    public record PesoProduto(@jakarta.validation.constraints.DecimalMin("0") @Schema(example = "12.5") java.math.BigDecimal pesoBrutoKg,
                              @Min(1) Integer itensPorPalete) {
    }

    public record ParametroDescarga(@NotNull @Min(1) @Max(1440) @Schema(example = "60") Integer minutosPrevistos,
                                    @NotNull @Min(0) @Max(1440) @Schema(example = "30") Integer toleranciaEsperaMinutos,
                                    @NotNull @jakarta.validation.constraints.DecimalMin("0") @Schema(example = "3.50") java.math.BigDecimal custoPorPalete) {
    }

    public record Cobranca(@NotBlank @Size(max = 80) @Schema(example = "Bebidas Vale Ltda") String fornecedor,
                           @NotNull @Min(1) @Max(100000) @Schema(example = "20") Integer quantidade,
                           @NotNull @jakarta.validation.constraints.DecimalMin("0") @Schema(example = "45.00") java.math.BigDecimal valorUnitario,
                           @NotNull @Schema(example = "2026-11-10") LocalDate vencimento,
                           @Size(max = 255) String observacao) {
    }

    public record Pagamento(@NotNull @Schema(example = "PIX") br.com.redekrill.agendamento.domain.model.CobrancaPalete.FormaPagamento forma) {
    }

    public record Movimento(LocalDate data,
                            @NotNull @Schema(example = "ENVIO") br.com.redekrill.agendamento.domain.model.MovimentoPalete.Tipo tipo,
                            Long cdId, @NotBlank @Size(max = 80) @Schema(example = "Loja 12 - Centro") String loja,
                            @NotNull @Min(0) @Max(10000) Integer pbr, @NotNull @Min(0) @Max(10000) Integer chep,
                            @NotNull @Min(0) @Max(10000) Integer chapatex, @NotNull @Min(0) @Max(10000) Integer descartaveis,
                            @Size(max = 255) String observacao) {
    }

    public record Cancelar(@NotBlank(message = "Motivo do cancelamento é obrigatório")
                           @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres") String motivo) {
    }

    public record Simular(@NotNull LocalDate data, Long faixaHorariaId, Long tipoPaletizacaoId,
                          @Min(0) @Max(10000) Integer quantidadePaletes, Long cdId) {
    }

    public record Parametro(@NotNull @Min(1) @Max(100000) Integer capacidadeDiariaPaletes,
                            @NotNull @Min(1) @Max(24) Integer quantidadeFaixas,
                            @NotNull @Min(15) @Max(720) Integer duracaoFaixaMinutos) {
    }

    public record Faixa(@NotNull(message = "Hora inicial é obrigatória") @Schema(type = "string", example = "08:00") LocalTime horaInicio,
                        @NotNull(message = "Hora final é obrigatória") @Schema(type = "string", example = "10:00") LocalTime horaFim,
                        @NotNull @Min(1) @Max(100000) Integer capacidadePaletes,
                        Boolean ativo) {
    }

    public record TipoCaminhao(@NotBlank(message = "Nome é obrigatório") @Size(max = 40) String nome,
                               @Size(max = 120) String descricao,
                               @Min(1) @Max(1000) Integer capacidadePaletes,
                               Boolean ativo) {
    }

    public record TipoPaletizacao(@NotBlank(message = "Nome é obrigatório") @Size(max = 40) String nome,
                                  @NotNull @Min(1) @Max(10) Integer fatorMultiplicador,
                                  Boolean ativo) {
    }

    public record AcaoRecebimento(
            @NotNull @Schema(description = "CONFIRMAR, CHEGADA, INICIAR, CONCLUIR ou NAO_COMPARECEU", example = "CHEGADA")
            br.com.redekrill.agendamento.application.usecase.RecebimentoUseCase.Acao acao,
            @Size(max = 8) @Schema(example = "ABC1D23") String placa,
            @Size(max = 80) @Schema(example = "João Pereira") String motorista,
            @Size(max = 80) @Schema(example = "Carlos (conferente)") String conferente,
            @Min(0) @Max(10000) Integer paletesRecebidos,
            @Size(max = 255) String divergencia,
            @Size(max = 255) String motivo) {
    }

    public record VeiculoFornecedor(@Size(max = 8) @Schema(example = "ABC1D23") String placa,
                                    @Size(max = 80) @Schema(example = "João Pereira") String motorista) {
    }

    public record NovoVale(Long agendamentoId, LocalDate data,
                           @Size(max = 80) @Schema(example = "Distribuidora Litoral Alimentos Ltda") String fornecedor,
                           @Size(max = 8) String placa, @Size(max = 80) String motorista,
                           @NotNull @Min(0) @Max(1000) @Schema(example = "20") Integer pbr,
                           @NotNull @Min(0) @Max(1000) Integer chep,
                           @NotNull @Min(0) @Max(1000) Integer chapatex,
                           @NotNull @Min(0) @Max(1000) Integer descartaveis,
                           @Size(max = 500) String observacao) {
    }

    public record Assinatura(@NotBlank @Size(max = 80) String nome,
                             @NotBlank @Size(max = 300000) @Schema(description = "PNG em data URL (data:image/png;base64,...)") String imagem) {
    }

    public record Devolucao(@NotNull @Min(0) Integer pbr, @NotNull @Min(0) Integer chep,
                            @NotNull @Min(0) Integer chapatex, @NotNull @Min(0) Integer descartaveis) {
    }

    public record InscricaoPush(@NotBlank @Size(max = 1000) String endpoint,
                                @NotBlank @Size(max = 255) String p256dh,
                                @NotBlank @Size(max = 255) String auth) {
    }
}
