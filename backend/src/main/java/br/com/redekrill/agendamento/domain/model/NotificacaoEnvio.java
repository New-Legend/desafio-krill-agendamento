package br.com.redekrill.agendamento.domain.model;

import java.time.OffsetDateTime;

/** Registro de um envio (ou link gerado) ao fornecedor, por e-mail ou WhatsApp. */
public record NotificacaoEnvio(Long id, Canal canal, String documentoTipo, Long documentoId, String destinatario,
                               String assunto, String provedor, Status status, String erro, String usuario,
                               OffsetDateTime criadoEm) {
    public enum Canal { EMAIL, WHATSAPP }

    public enum Status { ENVIADO, FALHOU, NAO_CONFIGURADO, LINK_GERADO }
}
