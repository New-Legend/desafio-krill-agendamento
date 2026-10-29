package br.com.redekrill.agendamento.application.port;

/**
 * Canal WhatsApp plugável. O padrão é o link wa.me (clique para conversar, aberto pelo usuário);
 * um adaptador de API do WhatsApp Business pode ser ligado por variável de ambiente.
 */
public interface NotificadorWhatsApp {
    record Resultado(boolean enviado, String erro) {
    }

    /** true quando há um provedor de API configurado para envio automático. */
    boolean habilitado();

    String provedor();

    /** telefone só com dígitos, com DDI (ex.: 5511999990000). */
    Resultado enviar(String telefone, String mensagem);
}
