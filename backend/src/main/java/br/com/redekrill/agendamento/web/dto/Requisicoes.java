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
            @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres") String observacao) {
    }

    public record Cancelar(@NotBlank(message = "Motivo do cancelamento é obrigatório")
                           @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres") String motivo) {
    }

    public record Simular(@NotNull LocalDate data, Long faixaHorariaId, Long tipoPaletizacaoId,
                          @Min(0) @Max(10000) Integer quantidadePaletes) {
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
