package br.com.redekrill.agendamento.domain.service;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o XML da NF-e (nfeProc ou NFe, layout 4.00) com parser protegido contra XXE.
 * Usa apenas o JDK, então a regra fica testável sem framework.
 */
public final class LeitorNfeXml {

    public static final int TAMANHO_MAXIMO = 2 * 1024 * 1024;

    public NotaFiscal ler(byte[] xml) {
        if (xml == null || xml.length == 0) throw new RegraNegocioException("Arquivo XML vazio");
        if (xml.length > TAMANHO_MAXIMO) throw new RegraNegocioException("XML maior que 2 MB");
        Document doc = parse(xml);
        Element inf = primeiro(doc.getDocumentElement(), "infNFe");
        if (inf == null) throw new RegraNegocioException("Arquivo não é uma NF-e (infNFe não encontrado)");
        String chave = inf.getAttribute("Id").replaceFirst("^NFe", "");
        if (!chave.matches("\\d{44}")) throw new RegraNegocioException("Chave de acesso da NF-e inválida");
        Element ide = primeiro(inf, "ide");
        Element emit = primeiro(inf, "emit");
        Element total = primeiro(primeiro(inf, "total"), "ICMSTot");
        Element vol = primeiro(primeiro(inf, "transp"), "vol");
        List<NotaFiscal.Item> itens = new ArrayList<>();
        NodeList dets = inf.getElementsByTagNameNS("*", "det");
        for (int i = 0; i < dets.getLength(); i++) {
            Element det = (Element) dets.item(i);
            Element prod = primeiro(det, "prod");
            if (prod == null) continue;
            String ean = texto(prod, "cEAN");
            if (ean != null && !ean.matches("\\d{8,14}")) ean = null;
            itens.add(new NotaFiscal.Item(numero(det.getAttribute("nItem"), i + 1), obrig(texto(prod, "cProd"), "cProd"),
                    ean, corta(obrig(texto(prod, "xProd"), "xProd"), 120), texto(prod, "NCM"), corta(texto(prod, "uCom"), 6),
                    decimal(texto(prod, "qCom")), decimal(texto(prod, "vUnCom")), decimal(texto(prod, "vProd")), null, null));
        }
        if (itens.isEmpty()) throw new RegraNegocioException("NF-e sem itens");
        return new NotaFiscal(null, null, chave, obrig(texto(ide, "nNF"), "nNF"), texto(ide, "serie"), texto(emit, "CNPJ"),
                corta(texto(emit, "xNome"), 120), dataHora(texto(ide, "dhEmi")), decimal(texto(total, "vNF")),
                decimal(texto(vol, "pesoB")), vol == null ? null : inteiro(texto(vol, "qVol")), itens, null, null);
    }

    private static Document parse(byte[] xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new RegraNegocioException("XML inválido ou não permitido");
        }
    }

    private static Element primeiro(Element pai, String tag) {
        if (pai == null) return null;
        NodeList l = pai.getElementsByTagNameNS("*", tag);
        return l.getLength() == 0 ? null : (Element) l.item(0);
    }

    private static String texto(Element pai, String tag) {
        if (pai == null) return null;
        for (Node n = pai.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && tag.equals(e.getLocalName())) {
                String t = e.getTextContent().trim();
                return t.isEmpty() ? null : t;
            }
        }
        return null;
    }

    private static String obrig(String v, String tag) {
        if (v == null) throw new RegraNegocioException("NF-e sem o campo " + tag);
        return v;
    }

    private static String corta(String v, int max) {
        return v == null || v.length() <= max ? v : v.substring(0, max);
    }

    private static BigDecimal decimal(String v) {
        try {
            return v == null ? null : new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Valor numérico inválido na NF-e: " + v);
        }
    }

    private static Integer inteiro(String v) {
        BigDecimal d = decimal(v);
        return d == null ? null : d.intValue();
    }

    private static int numero(String v, int padrao) {
        return v == null || !v.matches("\\d+") ? padrao : Integer.parseInt(v);
    }

    private static OffsetDateTime dataHora(String v) {
        try {
            return v == null ? null : OffsetDateTime.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
