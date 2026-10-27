package br.com.redekrill.agendamento.application.port;

public interface NotificadorPush {
    void notificarTodos(String titulo, String mensagem, String url);
}
