package com.small.langchain.client.pldft.enquadramento;

/**
 * Enquadramento cadastral da pessoa monitorada -- se e PEP, PEM e/ou funcionaria da instituicao.
 *
 * <p>A formatacao dos textos mora aqui, e nao em quem consulta, porque existem duas abordagens
 * para obter esse dado ({@link EnquadramentoStrategy}) e as duas precisam alimentar o prompt com
 * a mesma redacao. Formato divergente entre elas tornaria a comparacao entre as abordagens
 * inutil -- a diferenca de resultado poderia vir do texto, nao da abordagem.
 */
public record Enquadramento(String nome, boolean pep, boolean pem, boolean funcionario) {

    private static final String PREFIXO = "Pessoa monitorada: ";

    /** Linha unica com os tres aspectos -- usada pela consulta direta. */
    public String textoCompleto() {
        return PREFIXO + nome
                + " | PEP (Pessoa Politicamente Exposta): " + simOuNao(pep)
                + " | PEM (Pessoa Exposta na Mídia): " + simOuNao(pem)
                + " | Funcionário da instituição: " + simOuNao(funcionario);
    }

    /** Cada tool responde por um aspecto e precisa ser auto-contida, daí o nome se repetir. */
    public String textoPep() {
        return PREFIXO + nome + " | PEP (Pessoa Politicamente Exposta): " + simOuNao(pep);
    }

    public String textoPem() {
        return PREFIXO + nome + " | PEM (Pessoa Exposta na Mídia): " + simOuNao(pem);
    }

    public String textoFuncionario() {
        return PREFIXO + nome + " | Funcionário da instituição: " + simOuNao(funcionario);
    }

    private static String simOuNao(boolean valor) {
        return valor ? "sim" : "não";
    }
}
