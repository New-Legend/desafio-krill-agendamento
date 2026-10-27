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

    public record Login(@NotBlank String usuario, @NotBlank String senha) {
    }

    public record CriarAgendamento(
            @NotNull(message = "Data é obrigatória") LocalDate data,
            @NotNull(message = "Faixa horária é obrigatória") Long faixaHorariaId,
            @NotNull(message = "ID do pedido é obrigatório") @Positive(message = "ID do pedido deve ser positivo") Long idPedido,
            @NotBlank(message = "Fornecedor é obrigatório") @Size(max = 80, message = "Fornecedor deve ter no máximo 80 caracteres") String fornecedor,
            @NotBlank(message = "E-mail do fornecedor é obrigatório") @Size(max = 80, message = "E-mail deve ter no máximo 80 caracteres")
            @Email(message = "E-mail do fornecedor inválido") String emailFornecedor,
            @NotNull(message = "Tipo de caminhão é obrigatório") Long tipoCaminhaoId,
            @NotNull(message = "Tipo de paletização é obrigatório") Long tipoPaletizacaoId,
            @NotNull(message = "Quantidade de paletes é obrigatória") @Min(value = 1, message = "Quantidade de paletes deve ser maior que zero")
            @Max(value = 10000, message = "Quantidade de paletes acima do permitido") Integer quantidadePaletes,
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

    public record InscricaoPush(@NotBlank @Size(max = 1000) String endpoint,
                                @NotBlank @Size(max = 255) String p256dh,
                                @NotBlank @Size(max = 255) String auth) {
    }
}
