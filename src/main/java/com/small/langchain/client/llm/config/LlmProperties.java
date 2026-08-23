package com.small.langchain.client.llm.config;

import com.small.langchain.client.llm.model.ModelSpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuracao externalizada de tudo que fala com o provedor de LLM. Tirar esses valores
 * do codigo e o primeiro passo pra conseguir trocar de provedor (LM Studio, Ollama, OpenAI,
 * Azure...) sem recompilar: a API do langchain4j e a mesma, so muda a URL e o nome do modelo.
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(

        /** Endpoint compativel com a API da OpenAI. Local por padrao (LM Studio). */
        @DefaultValue("http://127.0.0.1:1234/v1") String baseUrl,

        /** LM Studio ignora a chave, mas o cliente OpenAI exige algum valor. */
        @DefaultValue("lm-studio") String apiKey,

        @DefaultValue("google/gemma-4-e2b") String defaultModel,

        /** Modelo de embeddings usado pelo RAG; precisa estar carregado no provedor. */
        @DefaultValue("text-embedding-nomic-embed-text-v1.5") String embeddingModel,

        @DefaultValue("0.3") Double defaultTemperature,

        @DefaultValue("1200") Integer defaultMaxTokens,

        /**
         * Modelos locais "raciocinam" antes de responder e podem consumir boa parte do
         * max_tokens so com isso, o que deixa a geracao bem mais lenta que uma API hospedada.
         */
        @DefaultValue("5m") Duration timeout,

        @DefaultValue("1") Integer maxRetries,

        /**
         * Modelos locais pequenos tendem a entrar em loop repetindo o mesmo trecho ate
         * estourar o max_tokens; penalizar repeticao reduz bastante isso.
         */
        @DefaultValue("0.6") Double frequencyPenalty,

        @DefaultValue("0.4") Double presencePenalty,

        /**
         * Quando um prompt pede um modelo que nao esta carregado no provedor, a chamada
         * cai automaticamente no modelo padrao em vez de estourar erro.
         */
        @DefaultValue("true") boolean fallbackToDefaultModel,

        /**
         * Tamanho do trecho na indexacao do RAG. Trecho grande demais dilui o assunto e piora a
         * similaridade; pequeno demais corta a frase antes da conclusao.
         */
        @DefaultValue("700") Integer ragChunkSize,

        /** Sobreposicao entre trechos vizinhos, pra ideia cortada na borda nao se perder. */
        @DefaultValue("120") Integer ragChunkOverlap,

        @DefaultValue("4") Integer ragMaxResults,

        /** Abaixo disso o trecho e ruido; devolver nada e melhor que devolver o irrelevante. */
        @DefaultValue("0.55") Double ragMinScore
) {

    public ModelSpec defaultSpec() {
        return new ModelSpec(defaultModel, defaultTemperature, defaultMaxTokens);
    }
}
