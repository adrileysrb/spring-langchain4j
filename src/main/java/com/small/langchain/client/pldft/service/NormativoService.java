package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.guardrail.GuardrailChain;
import com.small.langchain.client.llm.guardrail.rules.NaoVazioGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemRecusaGuardrail;
import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.rag.KnowledgeBase;
import com.small.langchain.client.llm.rag.KnowledgeBaseException;
import com.small.langchain.client.llm.rag.TrechoRecuperado;
import com.small.langchain.client.pldft.dto.RespostaNormativoDto;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Responde perguntas sobre normativos de PLDFT com RAG: recupera os trechos relevantes e so
 * entao gera a resposta, restrita ao que foi recuperado.
 *
 * <p>O ponto central esta na instrucao do prompt: o modelo e proibido de responder pelo que
 * "sabe" e obrigado a admitir quando o material nao cobre a pergunta. Sem esse limite o RAG
 * vira enfeite -- o modelo completa a lacuna com memoria de treino, e alucinacao sobre norma
 * e o tipo de erro que ninguem percebe ate dar problema.
 */
@Service
public class NormativoService {

    private static final GuardrailChain GUARDRAILS = GuardrailChain.de(
            new NaoVazioGuardrail(), new SemRecusaGuardrail());

    private final KnowledgeBase knowledgeBase;
    private final ChatModelFactory chatModelFactory;

    public NormativoService(KnowledgeBase knowledgeBase, ChatModelFactory chatModelFactory) {
        this.knowledgeBase = knowledgeBase;
        this.chatModelFactory = chatModelFactory;
    }

    /** Busca semantica pura, sem passar pelo modelo -- util pra inspecionar o que o RAG recupera. */
    public List<TrechoRecuperado> buscar(String pergunta) {
        validarPergunta(pergunta);
        try {
            return knowledgeBase.buscar(pergunta);
        } catch (KnowledgeBaseException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }

    public RespostaNormativoDto perguntar(String pergunta) {
        List<TrechoRecuperado> trechos = buscar(pergunta);
        if (trechos.isEmpty()) {
            return new RespostaNormativoDto(
                    "Não encontrei nada na base de normativos que responda a essa pergunta.", List.of());
        }

        ChatModel chatModel = chatModelFactory.defaultModel();
        ChatRequest request = ChatRequest.builder()
                .messages(
                        SystemMessage.from("Você responde dúvidas de analistas de PLDFT usando EXCLUSIVAMENTE "
                                + "os trechos de normativos fornecidos. Não use conhecimento próprio. Se os "
                                + "trechos não responderem à pergunta, diga exatamente isso. Cite entre "
                                + "colchetes o número do trecho que sustenta cada afirmação. Responda em "
                                + "português, de forma objetiva."),
                        UserMessage.from("Trechos disponíveis:\n\n" + numerar(trechos)
                                + "\n\nPergunta: " + pergunta))
                .build();

        ChatResponse response;
        try {
            response = LlmTaskContext.executando(
                    TarefaIa.CONSULTA_NORMATIVO, () -> chatModel.chat(request));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o modelo de IA: " + e.getMessage(), e);
        }

        String resposta = response.aiMessage().text();
        try {
            GUARDRAILS.exigir(resposta);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
        return new RespostaNormativoDto(resposta, trechos);
    }

    public void reindexar() {
        try {
            knowledgeBase.reindexar();
        } catch (KnowledgeBaseException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }

    private String numerar(List<TrechoRecuperado> trechos) {
        return IntStream.range(0, trechos.size())
                .mapToObj(i -> "[" + (i + 1) + "] (" + trechos.get(i).fonte() + ")\n" + trechos.get(i).texto())
                .collect(Collectors.joining("\n\n"));
    }

    private void validarPergunta(String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta não informada");
        }
    }
}
