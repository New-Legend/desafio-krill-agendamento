package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

/** Cadastro de fornecedor (porte de /recebimento-fornecedores). CNPJ guardado só com dígitos. */
public record Fornecedor(Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
                         String contato, boolean ativo) {

    public Fornecedor {
        cnpj = Textos.digitos(cnpj);
        if (!cnpjValido(cnpj)) throw new RegraNegocioException("CNPJ inválido");
        razaoSocial = Textos.obrigatorio(razaoSocial, "Razão social", 80);
        nomeFantasia = Textos.opcional(nomeFantasia, "Nome fantasia", 80);
        email = Textos.email(email, "E-mail", true);
        telefone = Textos.opcional(telefone, "Telefone", 20);
        contato = Textos.opcional(contato, "Contato", 80);
    }

    public String cnpjFormatado() {
        return cnpj.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    /** Validação dos dígitos verificadores do CNPJ. */
    public static boolean cnpjValido(String c) {
        if (c == null || !c.matches("\\d{14}") || c.chars().distinct().count() == 1) return false;
        return digito(c, 12) == c.charAt(12) - '0' && digito(c, 13) == c.charAt(13) - '0';
    }

    private static int digito(String c, int n) {
        int soma = 0;
        int peso = n - 7;
        for (int i = 0; i < n; i++) {
            soma += (c.charAt(i) - '0') * peso;
            peso = peso == 2 ? 9 : peso - 1;
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }
}
