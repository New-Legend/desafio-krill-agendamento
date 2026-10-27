package br.com.redekrill.agendamento.infrastructure.erp;

import br.com.redekrill.agendamento.application.port.PedidoCompraGateway;
import br.com.redekrill.agendamento.domain.model.PedidoCompraErp;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Adaptador Bluesoft ERP. OAuth2 client_credentials em {base}/{tenant}/oauth2/token e consulta em
 * GET {base}/{tenant}/api/compras/pedidos-de-compra. Nomes de campos do JSON a confirmar no Redoc do
 * tenant (ver docs/INTEGRACAO-BLUESOFT.md); a leitura é tolerante a variações.
 * Desligado por padrão: só carrega com app.erp.provedor=bluesoft.
 */
@Component
@ConditionalOnProperty(name = "app.erp.provedor", havingValue = "bluesoft")
public class PedidoCompraBluesoftGateway implements PedidoCompraGateway {

    private static final Logger log = LoggerFactory.getLogger(PedidoCompraBluesoftGateway.class);

    private final BluesoftProperties props;
    private final RestClient http;
    private volatile String token;
    private volatile Instant expiraEm = Instant.EPOCH;

    public PedidoCompraBluesoftGateway(BluesoftProperties props, RestClient.Builder builder) {
        if (props.tenant() == null || props.clientId() == null || props.clientSecret() == null) {
            throw new IllegalStateException("Bluesoft: defina BLUESOFT_TENANT, BLUESOFT_CLIENT_ID e BLUESOFT_CLIENT_SECRET");
        }
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        int t = (props.timeoutSegundos() == null ? 10 : props.timeoutSegundos()) * 1000;
        f.setConnectTimeout(t);
        f.setReadTimeout(t);
        this.http = builder.requestFactory(f).baseUrl(props.urlTenant()).build();
    }

    synchronized String token() {
        if (token != null && Instant.now().isBefore(expiraEm.minusSeconds(60))) return token;
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        JsonNode r = http.post().uri("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(JsonNode.class);
        token = r.path("access_token").asText();
        expiraEm = Instant.now().plusSeconds(r.path("expires_in").asLong(3600));
        return token;
    }

    @Override
    public Optional<PedidoCompraErp> buscarPedido(Long idPedido) {
        try {
            JsonNode r = http.get().uri(u -> u.path("/api/compras/pedidos-de-compra")
                            .queryParam("pedidoCompraKeys", idPedido).queryParam("page", 0).queryParam("size", 1).build())
                    .header("Authorization", "Bearer " + token()).retrieve().body(JsonNode.class);
            JsonNode p = primeiro(r);
            return p == null ? Optional.empty() : Optional.of(mapear(idPedido, p));
        } catch (RestClientException e) {
            log.warn("Bluesoft indisponível ao consultar pedido {}: {}", idPedido, e.getMessage());
            return Optional.empty();
        }
    }

    static JsonNode primeiro(JsonNode r) {
        if (r == null) return null;
        JsonNode lista = r.isArray() ? r : r.has("content") ? r.get("content") : r.path("data");
        return lista.isArray() && !lista.isEmpty() ? lista.get(0) : null;
    }

    static PedidoCompraErp mapear(Long id, JsonNode p) {
        JsonNode forn = p.has("fornecedor") ? p.get("fornecedor") : p;
        Integer paletes = p.has("quantidadePaletes") ? p.get("quantidadePaletes").asInt() : null;
        String data = texto(p, "dataPrevistaEntrega", "dataEntrega");
        return new PedidoCompraErp(id, texto(p, "situacao", "status"),
                texto(forn, "razaoSocial", "nome", "fornecedorNome"), texto(forn, "cpfCnpj", "cnpj"),
                texto(forn, "email", "emailContato"), paletes, null,
                texto(p, "lojaKey", "loja", "lojaDestino"), data == null ? null : LocalDate.parse(data.substring(0, 10)), "bluesoft");
    }

    private static String texto(JsonNode n, String... campos) {
        for (String c : campos) {
            if (n.hasNonNull(c) && !n.get(c).asText().isBlank()) return n.get(c).asText();
        }
        return null;
    }

    @Override
    public String provedor() {
        return "bluesoft";
    }
}
