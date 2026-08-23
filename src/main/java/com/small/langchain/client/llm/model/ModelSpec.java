package com.small.langchain.client.llm.model;

/**
 * Value Object com os parametros que identificam uma configuracao de modelo. Como e um record,
 * ganha equals/hashCode de graca e serve direto como chave de cache no {@link ChatModelFactory}
 * -- duas partes do sistema que pedem o mesmo modelo compartilham a mesma instancia.
 */
public record ModelSpec(String modelName, Double temperature, Integer maxTokens) {

    /**
     * Preenche o que veio nulo (tipico de configuracao vinda do banco, onde nem todo campo
     * e obrigatorio) com os padroes da aplicacao, garantindo que specs equivalentes gerem
     * a mesma chave de cache.
     */
    public ModelSpec withDefaults(String defaultModel, Double defaultTemperature, Integer defaultMaxTokens) {
        return new ModelSpec(
                modelName != null && !modelName.isBlank() ? modelName : defaultModel,
                temperature != null ? temperature : defaultTemperature,
                maxTokens != null ? maxTokens : defaultMaxTokens
        );
    }
}
