package com.small.langchain.client.llm.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * O loop de tool-calling escrito na mao com as APIs de baixo nivel do langchain4j
 * ({@link ChatModel} + {@code ToolSpecification}), sem {@code AiServices}.
 *
 * <p>O ciclo e sempre o mesmo e vale entender porque e assim: manda a conversa junto com as
 * specs das tools, recebe do modelo uma {@link AiMessage} que pode conter pedidos de execucao,
 * executa cada pedido localmente, devolve cada resultado como {@link ToolExecutionResultMessage}
 * na conversa e chama o modelo de novo -- agora com o resultado real em maos. Quando parar e
 * responsabilidade da {@link ToolLoopPolicy}.
 */
@Service
public class ToolLoopRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolLoopRunner.class);

    public ToolLoopResult executar(
            ChatModel chatModel,
            List<ChatMessage> mensagensIniciais,
            ToolRegistry tools,
            ToolLoopPolicy policy,
            int maxRodadas
    ) {
        List<ChatMessage> mensagens = new ArrayList<>(mensagensIniciais);
        List<ToolExecutionResultMessage> resultados = new ArrayList<>();
        Set<String> toolsJaChamadas = new LinkedHashSet<>();
        AiMessage ultimaResposta = null;

        for (int rodada = 1; rodada <= maxRodadas; rodada++) {
            ChatRequest request = ChatRequest.builder()
                    .messages(mensagens)
                    .toolSpecifications(tools.specifications())
                    .toolChoice(policy.toolChoice())
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            ultimaResposta = aiMessage;

            boolean chamouTool = aiMessage.hasToolExecutionRequests();
            if (chamouTool) {
                // A AiMessage com os pedidos precisa entrar na conversa antes dos resultados,
                // senao o provedor rejeita mensagens de tool "orfas".
                mensagens.add(aiMessage);
                for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                    log.info("Rodada {}/{}: chamando tool '{}' com args {}",
                            rodada, maxRodadas, toolRequest.name(), toolRequest.arguments());
                    String resultado = tools.execute(toolRequest);
                    log.info("Rodada {}/{}: resultado da tool '{}': {}",
                            rodada, maxRodadas, toolRequest.name(), resultado);

                    ToolExecutionResultMessage resultMessage =
                            ToolExecutionResultMessage.from(toolRequest, resultado);
                    resultados.add(resultMessage);
                    mensagens.add(resultMessage);
                    toolsJaChamadas.add(toolRequest.name());
                }
            } else {
                log.debug("Rodada {}/{}: modelo respondeu sem pedir ferramenta ({})",
                        rodada, maxRodadas, policy.descricao());
            }

            ToolLoopState estado = new ToolLoopState(
                    rodada, maxRodadas, toolsJaChamadas, tools.size(), chamouTool);
            if (!policy.deveContinuar(estado)) {
                // A ultima mensagem de texto e a resposta final e tambem faz parte da conversa;
                // rodadas que falharam em chamar tool ficam de fora pra nao poluir a proxima tentativa.
                if (!chamouTool) {
                    mensagens.add(aiMessage);
                }
                break;
            }
        }

        return new ToolLoopResult(mensagens, resultados, ultimaResposta);
    }
}
