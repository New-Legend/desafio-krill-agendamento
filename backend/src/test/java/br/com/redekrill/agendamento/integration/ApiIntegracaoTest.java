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
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
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

        mvc.perform(get("/api/v1/agendamentos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"admin\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());

        // USUARIO só visualiza
        mvc.perform(post("/api/v1/agendamentos").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                .content(agendamento(3, 1, 10))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auditoria").header("Authorization", usuario)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/parametros/capacidade").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capacidadeDiariaPaletes\":500,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"))
                .andExpect(status().isForbidden());

        // validação
        mvc.perform(post("/api/v1/agendamentos").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailFornecedor\":\"invalido\",\"fornecedor\":\"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(8));

        // não paletizado conta em dobro e excede a faixa (só alerta)
        String criado = mvc.perform(post("/api/v1/agendamentos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(agendamento(3, 2, 70)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamento.paletesEquivalentes").value(140))
                .andExpect(jsonPath("$.agendamento.faixaHoraria").value("13:00 - 15:00"))
                .andExpect(jsonPath("$.alertas[0].tipo").value("FAIXA"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(criado).at("/agendamento/id").asLong();
        verify(mailSender, timeout(3000)).send(any(MimeMessage.class));

        mvc.perform(get("/api/v1/ocupacao").param("data", dia.toString()).header("Authorization", usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paletesOcupados").value(140))
                .andExpect(jsonPath("$.percentual").value(29.2))
                .andExpect(jsonPath("$.faixas[2].percentual").value(116.7));

        mvc.perform(get("/api/v1/ocupacao/periodo").param("inicio", dia.toString()).param("fim", dia.plusDays(2).toString())
                .header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(3));

        mvc.perform(post("/api/v1/agendamentos/simulacao").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + dia + "\",\"faixaHorariaId\":3,\"tipoPaletizacaoId\":1,\"quantidadePaletes\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alertas.length()").value(1));

        mvc.perform(get("/api/v1/agendamentos").param("inicio", dia.toString()).param("fim", dia.toString())
                        .param("fornecedor", "beta").header("Authorization", usuario))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/v1/relatorios/agendamentos.csv").param("inicio", dia.toString()).param("fim", dia.toString())
                        .header("Authorization", usuario))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Distribuidora Beta")));

        mvc.perform(patch("/api/v1/agendamentos/" + id + "/cancelamento").header("Authorization", usuario)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Pedido cancelado pelo comprador\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/cancelamento").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"de novo\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/agendamentos/999999").header("Authorization", admin)).andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/ocupacao").param("data", dia.toString()).header("Authorization", admin))
                .andExpect(jsonPath("$.paletesOcupados").value(0));

        String aud = mvc.perform(get("/api/v1/auditoria").param("tamanho", "100").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode itens = json.readTree(aud).get("itens");
        assertThat(itens.findValuesAsText("acao")).contains("LOGIN", "LOGIN_FALHA", "CRIAR", "CANCELAR", "EXPORTAR");
        mvc.perform(get("/api/v1/auditoria").param("acao", "login_falha").param("usuario", "ADMIN")
                        .param("entidade", "sessao").param("inicio", LocalDate.now().minusDays(1).toString())
                        .param("fim", LocalDate.now().plusDays(1).toString()).header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].sucesso").value(false));
    }

    @Test
    void cadastros() throws Exception {
        String admin = token("admin", "admin123");
        mvc.perform(get("/api/v1/parametros/capacidade").header("Authorization", admin))
                .andExpect(jsonPath("$.capacidadeDiariaPaletes").value(480))
                .andExpect(jsonPath("$.quantidadeFaixas").value(4));
        mvc.perform(get("/api/v1/faixas-horarias").header("Authorization", admin)).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/v1/tipos-caminhao").header("Authorization", admin)).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/tipos-paletizacao").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.nome=='Não paletizado')].fatorMultiplicador").value(2));

        mvc.perform(post("/api/v1/faixas-horarias").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaInicio\":\"17:00\",\"horaFim\":\"19:00\",\"capacidadePaletes\":120}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Limite de 4 faixas ativas atingido"));
        mvc.perform(put("/api/v1/faixas-horarias/4").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horaInicio\":\"15:00\",\"horaFim\":\"17:00\",\"capacidadePaletes\":100,\"ativo\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacidadePaletes").value(100));

        mvc.perform(post("/api/v1/tipos-caminhao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Bitrem\",\"capacidadePaletes\":48}")).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/tipos-caminhao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"truck\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/tipos-caminhao/3").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Van\",\"capacidadePaletes\":6,\"ativo\":true}")).andExpect(jsonPath("$.capacidadePaletes").value(6));

        mvc.perform(post("/api/v1/tipos-paletizacao").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Caixaria\",\"fatorMultiplicador\":3}")).andExpect(status().isCreated());
        mvc.perform(put("/api/v1/tipos-paletizacao/1").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Paletizado\",\"fatorMultiplicador\":1,\"ativo\":true}")).andExpect(status().isOk());

        mvc.perform(put("/api/v1/parametros/capacidade").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacidadeDiariaPaletes\":500,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacidadeDiariaPaletes").value(500));
        mvc.perform(put("/api/v1/parametros/capacidade").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capacidadeDiariaPaletes\":480,\"quantidadeFaixas\":4,\"duracaoFaixaMinutos\":120}"));
        mvc.perform(get("/api/v1/ocupacao").param("data", "invalida").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void push() throws Exception {
        String usuario = token("usuario", "usuario123");
        mvc.perform(get("/api/v1/push/chave-publica")).andExpect(status().isOk())
                .andExpect(jsonPath("$.chavePublica").isString());
        mvc.perform(post("/api/v1/push/inscricoes").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://push.invalid/abc\",\"p256dh\":\"BKey\",\"auth\":\"auth\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/push/inscricoes")
                .param("endpoint", "https://push.invalid/abc").header("Authorization", usuario)).andExpect(status().isNoContent());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void porteGestaoCd() throws Exception {
        String admin = token("admin", "admin123");
        String usuario = token("usuario", "usuario123");
        mvc.perform(get("/api/v1/erp/pedidos/450001").header("Authorization", usuario))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.fornecedorEmail").value("agenda@litoralalimentos.com.br"))
                .andExpect(jsonPath("$.tipoPaletizacaoId").value(1)).andExpect(jsonPath("$.provedor").value("local"));
        mvc.perform(get("/api/v1/erp/pedidos/1").header("Authorization", usuario)).andExpect(status().isNotFound());

        String criado = mvc.perform(post("/api/v1/agendamentos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(agendamento(1, 1, 10).replace(dia.toString(), dia.plusDays(1).toString()))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode c = json.readTree(criado);
        long id = c.at("/agendamento/id").asLong();
        String link = c.get("linkFornecedor").asText();
        String tk = link.substring(link.lastIndexOf('/') + 1);

        mvc.perform(get("/api/v1/publico/fornecedor/" + tk)).andExpect(status().isOk())
                .andExpect(jsonPath("$.editavel").value(true)).andExpect(jsonPath("$.emailFornecedor").doesNotExist());
        mvc.perform(post("/api/v1/publico/fornecedor/" + tk + "/confirmacao")).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        mvc.perform(put("/api/v1/publico/fornecedor/" + tk + "/veiculo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"bra-2e19\",\"motorista\":\"Ana\"}")).andExpect(jsonPath("$.placa").value("BRA2E19"));
        mvc.perform(get("/api/v1/publico/fornecedor/token-invalido-com-mais-de-vinte")).andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", usuario).contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"CHEGADA\",\"placa\":\"BRA2E19\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"CHEGADA\",\"placa\":\"BRA2E19\"}")).andExpect(jsonPath("$.status").value("CHEGOU"));
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"INICIAR\",\"conferente\":\"Carlos\"}")).andExpect(jsonPath("$.status").value("EM_RECEBIMENTO"));
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"CONCLUIR\",\"paletesRecebidos\":9}")).andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.divergencia").isString());
        mvc.perform(post("/api/v1/agendamentos/" + id + "/link-fornecedor").header("Authorization", admin))
                .andExpect(jsonPath("$.linkFornecedor").isString());
        mvc.perform(get("/api/v1/publico/fornecedor/" + tk)).andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/dashboard").param("data", dia.plusDays(1).toString()).header("Authorization", usuario))
                .andExpect(status().isOk()).andExpect(jsonPath("$.porStatus.RECEBIDO").value(1))
                .andExpect(jsonPath("$.ocupacao.capacidade").value(480));

        String vale = mvc.perform(post("/api/v1/vales-palete").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoId\":" + id + ",\"pbr\":10,\"chep\":0,\"chapatex\":0,\"descartaveis\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.placa").value("BRA2E19"))
                .andReturn().getResponse().getContentAsString();
        long valeId = json.readTree(vale).get("id").asLong();
        String codigo = json.readTree(vale).get("codigo").asText();
        String ass = "{\"nome\":\"Ana\",\"imagem\":\"data:image/png;base64,iVBORw0KGgo=\"}";
        mvc.perform(put("/api/v1/vales-palete/" + valeId + "/assinaturas/MOTORISTA").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(ass)).andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(put("/api/v1/vales-palete/" + valeId + "/assinaturas/CONFERENTE").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(ass)).andExpect(jsonPath("$.status").value("ASSINADO"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/vales-palete/" + valeId + "/assinaturas/CONFERENTE")
                .header("Authorization", admin)).andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(post("/api/v1/vales-palete/" + valeId + "/devolucoes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pbr\":10,\"chep\":0,\"chapatex\":0,\"descartaveis\":2}")).andExpect(jsonPath("$.status").value("QUITADO"));
        mvc.perform(get("/api/v1/vales-palete/codigo/" + codigo).header("Authorization", usuario)).andExpect(jsonPath("$.id").value(valeId));
        mvc.perform(get("/api/v1/vales-palete").param("status", "QUITADO").param("fornecedor", "beta").header("Authorization", usuario))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/vales-palete/" + valeId).header("Authorization", usuario)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/vales-palete/" + valeId + "/cancelamento").header("Authorization", admin)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void porteRecebimentoCompleto() throws Exception {
        String admin = token("admin", "admin123");
        String usuario = token("usuario", "usuario123");
        LocalDate d = dia.plusDays(2);
        var json_ = MediaType.APPLICATION_JSON;

        // multi-CD e fornecedor cadastrado
        mvc.perform(post("/api/v1/cds").header("Authorization", usuario).contentType(json_)
                .content("{\"codigo\":\"807\",\"nome\":\"CD 2\",\"ativo\":true}")).andExpect(status().isForbidden());
        long cd2 = json.readTree(mvc.perform(post("/api/v1/cds").header("Authorization", admin).contentType(json_)
                        .content("{\"codigo\":\"807\",\"nome\":\"CD 2\",\"capacidadeDiariaPaletes\":100,\"capacidadeFaixaPaletes\":30,\"ativo\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/cds").header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/v1/fornecedores").header("Authorization", admin).contentType(json_)
                .content("{\"cnpj\":\"11.222.333/0001-80\",\"razaoSocial\":\"X\",\"email\":\"x@x.com\",\"ativo\":true}"))
                .andExpect(status().isUnprocessableEntity());
        long forn = json.readTree(mvc.perform(post("/api/v1/fornecedores").header("Authorization", admin).contentType(json_)
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"razaoSocial\":\"Litoral Alimentos\",\"email\":\"agenda@litoral.com.br\",\"ativo\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        String corpo = agendamento(1, 1, 40).replace(dia.toString(), d.toString())
                .replace("\"observacao\"", "\"cdId\":" + cd2 + ",\"fornecedorId\":" + forn + ",\"observacao\"");
        JsonNode c = json.readTree(mvc.perform(post("/api/v1/agendamentos").header("Authorization", admin).contentType(json_).content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.alertas[0].tipo").value("FAIXA"))
                .andReturn().getResponse().getContentAsString());
        long id = c.at("/agendamento/id").asLong();
        assertThat(c.at("/agendamento/cdId").asLong()).isEqualTo(cd2);
        mvc.perform(get("/api/v1/ocupacao").param("data", d.toString()).param("cdId", String.valueOf(cd2)).header("Authorization", usuario))
                .andExpect(jsonPath("$.capacidade").value(100)).andExpect(jsonPath("$.paletesOcupados").value(40));
        mvc.perform(get("/api/v1/ocupacao").param("data", d.toString()).header("Authorization", usuario))
                .andExpect(jsonPath("$.capacidade").value(480)).andExpect(jsonPath("$.paletesOcupados").value(0));

        // fornecedor remarca pelo link: só faixa com vaga
        String link = c.get("linkFornecedor").asText();
        String tk = link.substring(link.lastIndexOf('/') + 1);
        mvc.perform(get("/api/v1/publico/fornecedor/" + tk + "/disponibilidade")).andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[0].capacidade").value(100));
        mvc.perform(put("/api/v1/publico/fornecedor/" + tk + "/reagendamento").contentType(json_)
                .content("{\"data\":\"" + d + "\",\"faixaHorariaId\":2}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/data").header("Authorization", admin).contentType(json_)
                .content("{\"data\":\"" + d.plusDays(1) + "\",\"faixaHorariaId\":2}"))
                .andExpect(jsonPath("$.agendamento.faixaHorariaId").value(2)).andExpect(jsonPath("$.alertas[0].tipo").value("FAIXA"));

        // recebimento + NF-e
        for (String a : new String[]{"{\"acao\":\"CHEGADA\",\"placa\":\"ABC1D23\"}", "{\"acao\":\"INICIAR\",\"conferente\":\"Carlos\"}"}) {
            mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", admin).contentType(json_).content(a)).andExpect(status().isOk());
        }
        byte[] xml = getClass().getResourceAsStream("/nfe/nfe-exemplo.xml").readAllBytes();
        var arquivo = new org.springframework.mock.web.MockMultipartFile("arquivo", "nfe.xml", "text/xml", xml);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/agendamentos/" + id + "/notas")
                        .file(arquivo).header("Authorization", usuario)).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/agendamentos/" + id + "/notas")
                        .file(arquivo).header("Authorization", admin)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.produtosCriados").value(2)).andExpect(jsonPath("$.nota.itens.length()").value(2));
        mvc.perform(get("/api/v1/agendamentos/" + id + "/notas").header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/produtos").param("semPeso", "true").header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(2));
        long prod = json.readTree(mvc.perform(get("/api/v1/produtos").header("Authorization", usuario)).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(put("/api/v1/produtos/" + prod + "/peso").header("Authorization", admin).contentType(json_)
                .content("{\"pesoBrutoKg\":5.2,\"itensPorPalete\":40}")).andExpect(jsonPath("$.pesoBrutoKg").value(5.2));
        mvc.perform(patch("/api/v1/agendamentos/" + id + "/status").header("Authorization", admin).contentType(json_)
                .content("{\"acao\":\"CONCLUIR\",\"paletesRecebidos\":38}")).andExpect(jsonPath("$.status").value("RECEBIDO"));

        // relatórios
        String ini = d.plusDays(1).toString();
        mvc.perform(get("/api/v1/divergencias").param("inicio", ini).param("fim", ini).header("Authorization", usuario))
                .andExpect(jsonPath("$[0].diferencaPaletes").value(-2)).andExpect(jsonPath("$[0].notas").value(1));
        mvc.perform(get("/api/v1/divergencias.csv").param("inicio", ini).param("fim", ini).header("Authorization", usuario))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")));
        mvc.perform(get("/api/v1/parametros/descarga").header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        mvc.perform(put("/api/v1/parametros/descarga/2").header("Authorization", admin).contentType(json_)
                .content("{\"minutosPrevistos\":45,\"toleranciaEsperaMinutos\":20,\"custoPorPalete\":2.5}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/relatorios/descarga").param("inicio", ini).param("fim", ini).header("Authorization", usuario))
                .andExpect(jsonPath("$.linhas.length()").value(1)).andExpect(jsonPath("$.resumo[0].recebimentos").value(1));
        mvc.perform(get("/api/v1/relatorios/descarga.csv").param("inicio", ini).param("fim", ini).header("Authorization", usuario)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rotina").header("Authorization", usuario)).andExpect(jsonPath("$.itens.length()").value(8));

        // conta-corrente, cobrança, recibo e lojas
        mvc.perform(post("/api/v1/vales-palete").header("Authorization", admin).contentType(json_)
                .content("{\"agendamentoId\":" + id + ",\"pbr\":20,\"chep\":0,\"chapatex\":0,\"descartaveis\":0}")).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/paletes/saldos").param("fornecedor", "beta").header("Authorization", usuario))
                .andExpect(jsonPath("$[0].saldoEmAberto").value(20));
        long cob = json.readTree(mvc.perform(post("/api/v1/paletes/cobrancas").header("Authorization", admin).contentType(json_)
                        .content("{\"fornecedor\":\"Distribuidora Beta\",\"quantidade\":5,\"valorUnitario\":45,\"vencimento\":\"" + d + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/v1/paletes/cobrancas/" + cob + "/pagamento").header("Authorization", admin).contentType(json_)
                .content("{\"forma\":\"PIX\"}")).andExpect(jsonPath("$.reciboNumero").value(org.hamcrest.Matchers.startsWith("RP-")));
        mvc.perform(get("/api/v1/paletes/cobrancas/" + cob).header("Authorization", usuario)).andExpect(jsonPath("$.status").value("PAGA"));
        mvc.perform(get("/api/v1/paletes/saldos").param("fornecedor", "beta").header("Authorization", usuario))
                .andExpect(jsonPath("$[0].saldoEmAberto").value(15));
        mvc.perform(post("/api/v1/paletes/movimentos").header("Authorization", admin).contentType(json_)
                        .content("{\"tipo\":\"ENVIO\",\"loja\":\"Loja 12\",\"pbr\":30,\"chep\":0,\"chapatex\":0,\"descartaveis\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.romaneio").value(org.hamcrest.Matchers.startsWith("RM-")));
        mvc.perform(get("/api/v1/paletes/lojas").header("Authorization", usuario)).andExpect(jsonPath("$[0].pbr").value(30));
        mvc.perform(get("/api/v1/paletes/movimentos").header("Authorization", usuario)).andExpect(jsonPath("$.length()").value(1));
    }
}
