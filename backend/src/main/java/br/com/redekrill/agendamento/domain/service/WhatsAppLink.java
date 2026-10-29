package br.com.redekrill.agendamento.domain.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Link "clique para conversar" do WhatsApp (https://wa.me/NUMERO?text=...). Não envia nada:
 * o usuário abre o link e confirma o envio no próprio WhatsApp.
 */
public final class WhatsAppLink {
    private WhatsAppLink() {
    }

    /**
     * Normaliza para DDI+DDD+número só com dígitos. Telefones brasileiros com 10 ou 11 dígitos
     * ganham o 55; números já com 55 (12/13 dígitos) ficam como estão. Inválido devolve null.
     */
    public static String normalizarTelefone(String telefone) {
        if (telefone == null) return null;
        String d = telefone.replaceAll("\\D", "").replaceFirst("^0+", "");
        if (d.length() == 10 || d.length() == 11) return "55" + d;
        if ((d.length() == 12 || d.length() == 13) && d.startsWith("55")) return d;
        return null;
    }

    /** Sem telefone válido o link abre o WhatsApp para o usuário escolher o contato. */
    public static String link(String telefone, String mensagem) {
        String numero = normalizarTelefone(telefone);
        String texto = URLEncoder.encode(mensagem == null ? "" : mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + (numero == null ? "" : numero) + "?text=" + texto;
    }
}
