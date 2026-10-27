package br.com.redekrill.agendamento.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste ponta a ponta da API com PostgreSQL real (Testcontainers) e migrações Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    final LocalDate dia = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(7);

    @BeforeEach
    void mail() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    String token(String usuario, String senha) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    String agendamento(int faixa, int paletizacao, int qtd) {
        return """
                {"data":"%s","faixaHorariaId":%d,"idPedido":98765,"fornecedor":"Distribuidora Beta",
                 "emailFornecedor":"logistica@beta.com.br","tipoCaminhaoId":2,"tipoPaletizacaoId":%d,
                 "quantidadePaletes":%d,"observacao":"Descarga lateral"}""".formatted(dia, faixa, paletizacao, qtd);
    }

    @Test
    void fluxoCompletoComPermissoesEAuditoria() throws Exception {
        String admin = token("admin", "admin123");
        String usuario = token("usuario", "usuario123");

        mvc.perform(get("/api/agendamentos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"admin\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());

        // USUARIO só visualiza
        mvc.perform(post("/api/agendamentos").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                .content(agendamento(3, 1, 10))).andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").header("Authorization", usuario)).andExpect(status().isForbidden());
        mvc.perform(put("/api/parametros/capacidade").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capacidadeDiariaPaletes\":500,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"))
                .andExpect(status().isForbidden());

        // validação
        mvc.perform(post("/api/agendamentos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailFornecedor\":\"invalido\",\"fornecedor\":\"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(8));

        // não paletizado conta em dobro e excede a faixa (só alerta)
        String criado = mvc.perform(post("/api/agendamentos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(agendamento(3, 2, 70)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamento.paletesEquivalentes").value(140))
                .andExpect(jsonPath("$.agendamento.faixaHoraria").value("13:00 - 15:00"))
                .andExpect(jsonPath("$.alertas[0].tipo").value("FAIXA"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(criado).at("/agendamento/id").asLong();
        verify(mailSender, timeout(3000)).send(any(MimeMessage.class));

        mvc.perform(get("/api/ocupacao").param("data", dia.toString()).header("Authorization", usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paletesOcupados").value(140))
                .andExpect(jsonPath("$.percentual").value(29.2))
                .andExpect(jsonPath("$.faixas[2].percentual").value(116.7));

        mvc.perform(get("/api/ocupacao/periodo").param("inicio", dia.toString()).param("fim", dia.plusDays(2).toString())
                .header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(3));

        mvc.perform(post("/api/agendamentos/simulacao").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + dia + "\",\"faixaHorariaId\":3,\"tipoPaletizacaoId\":1,\"quantidadePaletes\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alertas.length()").value(1));

        mvc.perform(get("/api/agendamentos").param("inicio", dia.toString()).param("fim", dia.toString())
                        .param("fornecedor", "beta").header("Authorization", usuario))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/relatorios/agendamentos.csv").param("inicio", dia.toString()).param("fim", dia.toString())
                        .header("Authorization", usuario))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Distribuidora Beta")));

        mvc.perform(patch("/api/agendamentos/" + id + "/cancelamento").header("Authorization", usuario)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/agendamentos/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Pedido cancelado pelo comprador\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(patch("/api/agendamentos/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"de novo\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/agendamentos/999999").header("Authorization", admin)).andExpect(status().isNotFound());

        mvc.perform(get("/api/ocupacao").param("data", dia.toString()).header("Authorization", admin))
                .andExpect(jsonPath("$.paletesOcupados").value(0));

        String aud = mvc.perform(get("/api/auditoria").param("tamanho", "100").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode itens = json.readTree(aud).get("itens");
        assertThat(itens.findValuesAsText("acao")).contains("LOGIN", "LOGIN_FALHA", "CRIAR", "CANCELAR", "EXPORTAR");
        mvc.perform(get("/api/auditoria").param("acao", "login_falha").param("usuario", "ADMIN")
                        .param("entidade", "sessao").param("inicio", LocalDate.now().minusDays(1).toString())
                        .param("fim", LocalDate.now().plusDays(1).toString()).header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].sucesso").value(false));
    }

    @Test
    void cadastros() throws Exception {
        String admin = token("admin", "admin123");
        mvc.perform(get("/api/parametros/capacidade").header("Authorization", admin))
                .andExpect(jsonPath("$.capacidadeDiariaPaletes").value(480))
                .andExpect(jsonPath("$.quantidadeFaixas").value(4));
        mvc.perform(get("/api/faixas-horarias").header("Authorization", admin)).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/tipos-caminhao").header("Authorization", admin)).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/tipos-paletizacao").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.nome=='Não paletizado')].fatorMultiplicador").value(2));

        mvc.perform(post("/api/faixas-horarias").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaInicio\":\"17:00\",\"horaFim\":\"19:00\",\"capacidadePaletes\":120}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Limite de 4 faixas ativas atingido"));
        mvc.perform(put("/api/faixas-horarias/4").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horaInicio\":\"15:00\",\"horaFim\":\"17:00\",\"capacidadePaletes\":100,\"ativo\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacidadePaletes").value(100));

        mvc.perform(post("/api/tipos-caminhao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Bitrem\",\"capacidadePaletes\":48}")).andExpect(status().isCreated());
        mvc.perform(post("/api/tipos-caminhao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"truck\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/tipos-caminhao/3").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Van\",\"capacidadePaletes\":6,\"ativo\":true}")).andExpect(jsonPath("$.capacidadePaletes").value(6));

        mvc.perform(post("/api/tipos-paletizacao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Caixaria\",\"fatorMultiplicador\":3}")).andExpect(status().isCreated());
        mvc.perform(put("/api/tipos-paletizacao/1").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Paletizado\",\"fatorMultiplicador\":1,\"ativo\":true}")).andExpect(status().isOk());

        mvc.perform(put("/api/parametros/capacidade").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacidadeDiariaPaletes\":500,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacidadeDiariaPaletes").value(500));
        mvc.perform(put("/api/parametros/capacidade").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capacidadeDiariaPaletes\":480,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"));
        mvc.perform(get("/api/ocupacao").param("data", "invalida").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push() throws Exception {
        String usuario = token("usuario", "usuario123");
        mvc.perform(get("/api/push/chave-publica")).andExpect(status().isOk())
                .andExpect(jsonPath("$.chavePublica").isString());
        mvc.perform(post("/api/push/inscricoes").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://push.invalid/abc\",\"p256dh\":\"BKey\",\"auth\":\"auth\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/push/inscricoes")
                .param("endpoint", "https://push.invalid/abc").header("Authorization", usuario)).andExpect(status().isNoContent());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
