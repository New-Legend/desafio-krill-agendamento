package br.com.redekrill.agendamento.application.port;

/** Envio síncrono de e-mail de documentos (cobrança/recibo de paletes), para registrar o resultado. */
public interface CanalEmailDocumentos {
    record Resultado(boolean enviado, String erro) {
    }

    String provedor();

    Resultado enviar(String para, String assunto, String html, String texto);
}
