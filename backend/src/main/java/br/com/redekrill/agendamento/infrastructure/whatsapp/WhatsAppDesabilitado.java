package br.com.redekrill.agendamento.infrastructure.whatsapp;

import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;

/** Padrão: nenhum envio automático; o sistema só gera o link wa.me para o usuário abrir. */
public class WhatsAppDesabilitado implements NotificadorWhatsApp {
    @Override
    public boolean habilitado() {
        return false;
    }

    @Override
    public String provedor() {
        return "wa.me";
    }

    @Override
    public Resultado enviar(String telefone, String mensagem) {
        return new Resultado(false, "Envio por API do WhatsApp não configurado");
    }
}
