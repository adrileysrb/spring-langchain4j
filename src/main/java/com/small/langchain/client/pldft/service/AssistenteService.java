package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.guardrail.GuardrailChain;
import com.small.langchain.client.llm.guardrail.rules.NaoVazioGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemRecusaGuardrail;
import com.small.langchain.client.llm.memory.ChatMemoryFactory;
import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.tool.ToolLoopPolicy;
import com.small.langchain.client.llm.tool.ToolLoopResult;
import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.dto.RespostaAssistenteDto;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.tool.OcorrenciaTools;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Assistente conversacional do analista sobre uma ocorrencia: junta memoria persistida e o loop
 * de tools em modo automatico, deixando o modelo decidir a cada pergunta se precisa consultar
 * algo no sistema.
 *
 * <p>E o contraste direto com os outros fluxos deste projeto. Ali a aplicacao dita o roteiro
 * (consulte estas tres tools, depois classifique); aqui quem conduz e a conversa, e o codigo so
 * garante limites -- janela de memoria, teto de rodadas e guardrails na saida.
 */
@Service
public class AssistenteService {

    /** Cada pergunta pode gastar algumas rodadas de tool antes da resposta final. */
    private static final int MAX_RODADAS = 5;

    /** Janela curta de proposito: modelo local pequeno perde qualidade com contexto longo. */
    private static final int MAX_MENSAGENS_NA_MEMORIA = 12;

    private static final GuardrailChain GUARDRAILS = GuardrailChain.de(
            new NaoVazioGuardrail(), new SemRecusaGuardrail());

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ChatModelFactory chatModelFactory;
    private final ChatMemoryFactory chatMemoryFactory;
    private final ToolLoopRunner toolLoopRunner;
    private final OcorrenciaTools ocorrenciaTools;

    public AssistenteService(
            OcorrenciaRepository ocorrenciaRepository,
            ChatModelFactory chatModelFactory,
            ChatMemoryFactory chatMemoryFactory,
            ToolLoopRunner toolLoopRunner,
            OcorrenciaTools ocorrenciaTools
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.chatModelFactory = chatModelFactory;
        this.chatMemoryFactory = chatMemoryFactory;
        this.toolLoopRunner = toolLoopRunner;
        this.ocorrenciaTools = ocorrenciaTools;
    }

    public RespostaAssistenteDto perguntar(Long ocorrenciaId, String pergunta) {
        if (pergunta == null || pergunta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta não informada");
        }
        if (ocorrenciaRepository.findById(ocorrenciaId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada");
        }

        String conversaId = conversaId(ocorrenciaId);
        ChatMemory memoria = chatMemoryFactory.paraConversa(conversaId, MAX_MENSAGENS_NA_MEMORIA);
        if (memoria.messages().isEmpty()) {
            memoria.add(mensagemDeSistema(ocorrenciaId));
        }
        memoria.add(UserMessage.from(pergunta));

        List<ChatMessage> historico = memoria.messages();
        ChatModel chatModel = chatModelFactory.defaultModel();

        ToolLoopResult resultado;
        try {
            resultado = LlmTaskContext.executando(
                    TarefaIa.ASSISTENTE_ANALISTA,
                    () -> toolLoopRunner.executar(
                            chatModel,
                            historico,
                            ocorrenciaTools.contextoCompleto(),
                            ToolLoopPolicy.ateOModeloParar(),
                            MAX_RODADAS));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o modelo de IA: " + e.getMessage(), e);
        }

        // Tudo que o loop produziu (tool calls, resultados e a resposta final) entra na memoria,
        // senao a proxima pergunta perde o que ja foi consultado e o modelo repete as consultas.
        resultado.mensagens().subList(historico.size(), resultado.mensagens().size())
                .forEach(memoria::add);

        String resposta = resultado.texto();
        try {
            GUARDRAILS.exigir(resposta);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }

        return new RespostaAssistenteDto(
                conversaId, resposta, resultado.ferramentasUsadas(), memoria.messages().size());
    }

    /** Zera a conversa sem apagar nada do caso -- util quando o modelo se perde no contexto. */
    public void limpar(Long ocorrenciaId) {
        chatMemoryFactory.paraConversa(conversaId(ocorrenciaId), MAX_MENSAGENS_NA_MEMORIA).clear();
    }

    private String conversaId(Long ocorrenciaId) {
        return "ocorrencia-" + ocorrenciaId;
    }

    private SystemMessage mensagemDeSistema(Long ocorrenciaId) {
        return SystemMessage.from("Você é um assistente de analistas de PLDFT (prevenção à lavagem de "
                + "dinheiro e financiamento ao terrorismo) e está apoiando a análise da ocorrência #"
                + ocorrenciaId + ". Use as ferramentas disponíveis sempre que a resposta depender de "
                + "dados do sistema (enquadramento cadastral, movimentações, alertas, histórico), "
                + "informando o id " + ocorrenciaId + ". Responda em português, de forma objetiva, "
                + "citando os números que as ferramentas retornarem. Nunca invente dados: se a "
                + "informação não estiver disponível, diga isso.");
    }
}
