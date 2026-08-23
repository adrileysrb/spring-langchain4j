package com.small.langchain.client.llm;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orquestra chamadas de tool usando diretamente as APIs de baixo nivel do langchain4j
 * ({@link ChatModel} + {@link dev.langchain4j.agent.tool.ToolSpecification}), sem
 * depender de {@code AiServices}: monta o {@link ChatRequest} com as tools disponiveis,
 * forca o modelo a chamar uma ferramenta a cada rodada e realimenta o resultado real
 * (vindo do cadastro) de volta na conversa via {@link ToolExecutionResultMessage}.
 */
@Service
public class ToolCallingChatService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingChatService.class);

    /**
     * Executa rodadas de tool-calling ate que todas as tools do registry tenham sido
     * chamadas pelo menos uma vez ou o limite de rodadas seja atingido. Cada rodada usa
     * {@link ToolChoice#REQUIRED} porque modelos locais pequenos as vezes "fingem" ter
     * usado uma tool escrevendo texto solto em vez de emitir um tool_call de verdade.
     *
     * @return os resultados reais de cada tool efetivamente chamada, na ordem em que foram executadas
     */
    public List<ToolExecutionResultMessage> executarAteTodasAsTools(
            ChatModel chatModel,
            List<ChatMessage> mensagensIniciais,
            ToolRegistry tools,
            int maxRodadas
    ) {
        List<ChatMessage> mensagens = new ArrayList<>(mensagensIniciais);
        List<ToolExecutionResultMessage> resultados = new ArrayList<>();
        Set<String> toolsJaChamadas = new HashSet<>();
        int totalDeTools = tools.specifications().size();

        for (int rodada = 1; rodada <= maxRodadas && toolsJaChamadas.size() < totalDeTools; rodada++) {
            ChatRequest request = ChatRequest.builder()
                    .messages(mensagens)
                    .toolSpecifications(tools.specifications())
                    .toolChoice(ToolChoice.REQUIRED)
                    .build();
            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();

            if (!aiMessage.hasToolExecutionRequests()) {
                log.warn("Rodada {}/{}: modelo não chamou nenhuma tool apesar de toolChoice=REQUIRED", rodada, maxRodadas);
                continue;
            }

            mensagens.add(aiMessage);
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                log.info("Rodada {}/{}: chamando tool '{}' com args {}",
                        rodada, maxRodadas, toolExecutionRequest.name(), toolExecutionRequest.arguments());
                String resultado = tools.execute(toolExecutionRequest);
                log.info("Rodada {}/{}: resultado da tool '{}': {}", rodada, maxRodadas, toolExecutionRequest.name(), resultado);

                ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, resultado);
                resultados.add(resultMessage);
                mensagens.add(resultMessage);
                toolsJaChamadas.add(toolExecutionRequest.name());
            }
        }
        return resultados;
    }
}
