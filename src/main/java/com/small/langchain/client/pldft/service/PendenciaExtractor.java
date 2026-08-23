package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.structured.StructuredExtractor;
import com.small.langchain.client.llm.structured.StructuredOutputClient;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.PendenciaItem;
import com.small.langchain.client.pldft.model.PendenciasExtraidas;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Segundo uso do mesmo Template Method, agora extraindo uma lista de objetos em vez de um unico
 * registro -- e o que mostra que o {@link StructuredExtractor} nao foi feito sob medida pra um caso.
 *
 * <p>Aqui o ganho pratico e direto: o rascunho do analista mistura fato, decisao e pendencia no
 * mesmo paragrafo corrido; virar lista tipada e o que permite transformar isso em tarefa.
 */
@Component
public class PendenciaExtractor extends StructuredExtractor<Analise, PendenciasExtraidas> {

    PendenciaExtractor(StructuredOutputClient client) {
        super(client);
    }

    @Override
    protected String nomeDoSchema() {
        return "pendencias_do_parecer";
    }

    @Override
    protected JsonObjectSchema schema() {
        JsonObjectSchema pendencia = JsonObjectSchema.builder()
                .addStringProperty("descricao", "o que ainda precisa ser feito, em uma frase")
                .addEnumProperty("tipo", PendenciaItem.TIPOS, "natureza da pendência")
                .addEnumProperty("prioridade", PendenciaItem.PRIORIDADES,
                        "urgência da pendência para a conclusão do caso")
                .required("descricao", "tipo", "prioridade")
                .additionalProperties(false)
                .build();

        return JsonObjectSchema.builder()
                .description("Pendências ainda em aberto citadas em um parecer de PLDFT")
                .addProperty("pendencias", JsonArraySchema.builder()
                        .description("lista das pendências; vazia se o parecer não deixar nada em aberto")
                        .items(pendencia)
                        .build())
                .required("pendencias")
                .additionalProperties(false)
                .build();
    }

    @Override
    protected List<ChatMessage> mensagens(Analise analise) {
        return List.of(
                SystemMessage.from("Você lê pareceres de analistas de PLDFT e extrai apenas as pendências "
                        + "ainda em aberto -- coisas que o próprio analista disse que faltam fazer. "
                        + "Não invente pendências e não inclua o que já foi concluído. Se nada estiver "
                        + "pendente, devolva uma lista vazia."),
                UserMessage.from("Parecer do analista " + analise.analista() + ":\n\n" + analise.parecer()
                        + "\n\nExtraia as pendências em aberto.")
        );
    }

    @Override
    protected Class<PendenciasExtraidas> tipoDaResposta() {
        return PendenciasExtraidas.class;
    }
}
