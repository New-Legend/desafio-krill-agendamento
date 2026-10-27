package br.com.redekrill.agendamento.infrastructure.push;

import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.ConfiguracaoEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.ConfiguracaoJpa;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

/**
 * Chaves VAPID do Web Push. Usa as variáveis de ambiente; se não houver, gera um par
 * uma única vez e guarda na tabela de configuração (as inscrições continuam válidas após reinício).
 */
@Component
public class ChavesVapid {

    private static final Logger log = LoggerFactory.getLogger(ChavesVapid.class);
    static final String CHAVE_PUBLICA = "vapid.public";
    static final String CHAVE_PRIVADA = "vapid.private";

    private final String publica;
    private final String privada;
    private final String subject;

    public ChavesVapid(AppProperties props, ConfiguracaoJpa config) {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.subject = props.push().subject();
        String pub = props.push().publicKey();
        String priv = props.push().privateKey();
        if (pub == null || pub.isBlank() || priv == null || priv.isBlank()) {
            pub = config.findById(CHAVE_PUBLICA).map(c -> c.valor).orElse(null);
            priv = config.findById(CHAVE_PRIVADA).map(c -> c.valor).orElse(null);
            if (pub == null || priv == null) {
                String[] par = gerar();
                pub = par[0];
                priv = par[1];
                salvar(config, CHAVE_PUBLICA, pub);
                salvar(config, CHAVE_PRIVADA, priv);
                log.info("Par de chaves VAPID gerado e armazenado na tabela configuracao");
            }
        }
        this.publica = pub;
        this.privada = priv;
    }

    static String[] gerar() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            g.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
            KeyPair kp = g.generateKeyPair();
            Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
            return new String[]{
                    enc.encodeToString(Utils.encode((ECPublicKey) kp.getPublic())),
                    enc.encodeToString(Utils.encode((ECPrivateKey) kp.getPrivate()))};
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar chaves VAPID", e);
        }
    }

    private static void salvar(ConfiguracaoJpa config, String chave, String valor) {
        ConfiguracaoEntity e = new ConfiguracaoEntity();
        e.chave = chave;
        e.valor = valor;
        config.save(e);
    }

    public String publica() {
        return publica;
    }

    String privada() {
        return privada;
    }

    String subject() {
        return subject;
    }
}
