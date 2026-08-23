package com.small.langchain.client.pldft.enquadramento;

import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.tool.ToolLoopPolicy;
import com.small.langchain.client.llm.tool.ToolLoopResult;
import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.tool.OcorrenciaTools;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Abordagem alternativa: o modelo obtem o enquadramento chamando uma tool por aspecto
 * (PEP, PEM, funcionario), com {@code ToolChoice.REQUIRED} e insistencia ate as tres responderem.
 *
 * <p>Ativa apenas com {@code pldft.enquadramento-via-tools=true}. Nao e o padrao porque, neste
 * fluxo especifico, custa chamadas de inferencia para chegar a um dado que uma query resolve --
 * veja {@link EnquadramentoStrategy} para o raciocinio completo.
 *
 * <p>Duas decisoes aqui merecem atencao, e valem para qualquer tool calling com modelo local:
 * <ul>
 *   <li>a chamada e <b>curta e isolada</b>, so com essa tarefa. Pedir para o modelo decidir usar
 *       ferramentas ao mesmo tempo que escreve um texto longo aumenta muito a chance de ele
 *       ignorar as ferramentas;</li>
 *   <li>a politica {@link ToolLoopPolicy#ateChamarTodasAsTools()} insiste porque modelo pequeno
 *       as vezes responde "consultei o cadastro e..." sem nunca emitir um {@code tool_call}.
 *       Quando o dado e obrigatorio, aceitar a primeira resposta seria aceitar uma invencao.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "pldft.enquadramento-via-tools", havingValue = "true")
public class EnquadramentoViaTools implements EnquadramentoStrategy {

    private static final Logger log = LoggerFactory.getLogger(EnquadramentoViaTools.class);

    /** Uma rodada por tool do registry de enquadramento. */
    private static final int MAX_RODADAS = 3;

    private final ToolLoopRunner toolLoopRunner;
    private final OcorrenciaTools ocorrenciaTools;

    EnquadramentoViaTools(ToolLoopRunner toolLoopRunner, OcorrenciaTools ocorrenciaTools) {
        this.toolLoopRunner = toolLoopRunner;
        this.ocorrenciaTools = ocorrenciaTools;
    }

    @Override
    public String nome() {
        return "via-tools";
    }

    @Override
    public String consultar(Ocorrencia ocorrencia, ChatModel chatModel) {
        List<ChatMessage> mensagens = List.of(
                SystemMessage.from("Voce verifica o enquadramento da pessoa monitorada de uma ocorrencia de PLDFT. "
                        + "Ha uma ferramenta para cada aspecto (PEP, PEM e funcionario da instituicao); use todas, "
                        + "informando o id da ocorrencia, ate ter consultado os tres. Nao escreva nenhum texto de "
                        + "resposta, apenas use as ferramentas."),
                UserMessage.from("Consulte o enquadramento completo (PEP, PEM e funcionario) da pessoa monitorada "
                        + "da ocorrencia #" + ocorrencia.id() + ".")
        );

        ToolLoopResult resultado = LlmTaskContext.executando(
                TarefaIa.CONSULTA_ENQUADRAMENTO,
                () -> toolLoopRunner.executar(
                        chatModel,
                        mensagens,
                        ocorrenciaTools.enquadramento(),
                        ToolLoopPolicy.ateChamarTodasAsTools(),
                        MAX_RODADAS));

        if (!resultado.algumaToolExecutada()) {
            log.warn("Ocorrência {}: nenhuma tool de enquadramento foi chamada de verdade, "
                    + "seguindo sem a informação", ocorrencia.id());
            return null;
        }
        return resultado.resultadosConcatenados("\n");
    }
}
