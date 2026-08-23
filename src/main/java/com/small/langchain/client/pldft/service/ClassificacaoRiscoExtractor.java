package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.structured.StructuredExtractor;
import com.small.langchain.client.llm.structured.StructuredOutputClient;
import com.small.langchain.client.pldft.model.ClassificacaoRisco;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classifica o risco de PLDFT de uma ocorrencia devolvendo dado estruturado em vez de texto
 * corrido -- o que permite gravar em coluna, ordenar fila de analise e disparar alcada, coisas
 * que um paragrafo em linguagem natural nao entrega.
 */
@Component
public class ClassificacaoRiscoExtractor extends StructuredExtractor<ContextoOcorrencia, ClassificacaoRisco> {

    ClassificacaoRiscoExtractor(StructuredOutputClient client) {
        super(client);
    }

    @Override
    protected String nomeDoSchema() {
        return "classificacao_risco_pldft";
    }

    @Override
    protected JsonObjectSchema schema() {
        return JsonObjectSchema.builder()
                .description("Classificação de risco de uma ocorrência de PLDFT")
                .addEnumProperty("risco", ClassificacaoRisco.NIVEIS,
                        "nível de risco atribuído ao caso")
                .addIntegerProperty("score",
                        "pontuação de 0 a 100, coerente com o nível de risco escolhido")
                .addStringProperty("justificativa",
                        "duas ou três frases explicando a classificação, citando apenas fatos informados")
                .addProperty("indicadores", JsonArraySchema.builder()
                        .description("indicadores objetivos que sustentam a classificação")
                        .items(new JsonStringSchema())
                        .build())
                .required("risco", "score", "justificativa", "indicadores")
                .additionalProperties(false)
                .build();
    }

    @Override
    protected List<ChatMessage> mensagens(ContextoOcorrencia contexto) {
        return List.of(
                SystemMessage.from("Você é um analista sênior de PLDFT (prevenção à lavagem de dinheiro e "
                        + "financiamento ao terrorismo). Classifique o risco do caso usando SOMENTE os fatos "
                        + "apresentados. Não invente informação que não esteja no material. Se os dados forem "
                        + "insuficientes para sustentar um risco alto, prefira um nível menor e diga isso na "
                        + "justificativa."),
                UserMessage.from("Ocorrência: #" + contexto.ocorrenciaId()
                        + "\nProduto: " + contexto.produto()
                        + "\nStatus: " + contexto.status()
                        + "\n\nFatos levantados no sistema:\n" + contexto.fatosLevantados()
                        + "\n\nÚltimo parecer registrado pelo analista:\n"
                        + (contexto.ultimoParecer() != null ? contexto.ultimoParecer() : "(nenhum parecer registrado)")
                        + "\n\nClassifique o risco deste caso.")
        );
    }

    @Override
    protected Class<ClassificacaoRisco> tipoDaResposta() {
        return ClassificacaoRisco.class;
    }
}
