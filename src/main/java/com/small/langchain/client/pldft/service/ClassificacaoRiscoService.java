package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.tool.ToolLoopPolicy;
import com.small.langchain.client.llm.tool.ToolLoopResult;
import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.ClassificacaoRisco;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.OcorrenciaClassificacao;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaClassificacaoRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.ProdutoRepository;
import com.small.langchain.client.pldft.tool.OcorrenciaTools;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encadeia duas capacidades: primeiro as tools levantam os fatos do caso no banco, depois a saida
 * estruturada transforma esses fatos numa classificacao com nivel, score e indicadores.
 *
 * <p>A ordem importa. Deixar o modelo classificar direto do texto do analista produziria um
 * palpite; grounding nos dados reais antes de julgar e o que torna a classificacao defensavel.
 */
@Service
public class ClassificacaoRiscoService {

    /** Uma rodada por tool do registry de contexto completo. */
    private static final int MAX_RODADAS_FATOS = 6;

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ProdutoRepository produtoRepository;
    private final AnaliseRepository analiseRepository;
    private final OcorrenciaClassificacaoRepository classificacaoRepository;
    private final ChatModelFactory chatModelFactory;
    private final ToolLoopRunner toolLoopRunner;
    private final OcorrenciaTools ocorrenciaTools;
    private final ClassificacaoRiscoExtractor extractor;

    public ClassificacaoRiscoService(
            OcorrenciaRepository ocorrenciaRepository,
            ProdutoRepository produtoRepository,
            AnaliseRepository analiseRepository,
            OcorrenciaClassificacaoRepository classificacaoRepository,
            ChatModelFactory chatModelFactory,
            ToolLoopRunner toolLoopRunner,
            OcorrenciaTools ocorrenciaTools,
            ClassificacaoRiscoExtractor extractor
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.produtoRepository = produtoRepository;
        this.analiseRepository = analiseRepository;
        this.classificacaoRepository = classificacaoRepository;
        this.chatModelFactory = chatModelFactory;
        this.toolLoopRunner = toolLoopRunner;
        this.ocorrenciaTools = ocorrenciaTools;
        this.extractor = extractor;
    }

    public OcorrenciaClassificacao classificar(Long ocorrenciaId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada"));

        ChatModel chatModel = chatModelFactory.defaultModel();
        String fatos = levantarFatos(chatModel, ocorrenciaId);

        ContextoOcorrencia contexto = new ContextoOcorrencia(
                ocorrenciaId,
                descricaoDoProduto(ocorrencia),
                ocorrencia.status(),
                fatos,
                ultimoParecer(ocorrenciaId)
        );

        ClassificacaoRisco classificacao;
        try {
            classificacao = LlmTaskContext.executando(
                    TarefaIa.CLASSIFICACAO_RISCO, () -> extractor.extrair(chatModel, contexto));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao classificar o risco com a IA: " + e.getMessage(), e);
        }

        return classificacaoRepository.save(new OcorrenciaClassificacao(
                null,
                ocorrenciaId,
                classificacao.risco(),
                normalizarScore(classificacao.score()),
                classificacao.justificativa(),
                classificacao.indicadores(),
                chatModel.defaultRequestParameters().modelName(),
                LocalDateTime.now()
        ));
    }

    public List<OcorrenciaClassificacao> listar(Long ocorrenciaId) {
        return classificacaoRepository.findByOcorrenciaId(ocorrenciaId);
    }

    private String levantarFatos(ChatModel chatModel, Long ocorrenciaId) {
        List<ChatMessage> mensagens = List.of(
                SystemMessage.from("Você coleta dados de uma ocorrência de PLDFT usando as ferramentas "
                        + "disponíveis. Use todas elas informando o id da ocorrência. Não escreva texto "
                        + "de resposta, apenas use as ferramentas."),
                UserMessage.from("Levante todos os dados disponíveis sobre a ocorrência #" + ocorrenciaId + ".")
        );

        ToolLoopResult resultado = LlmTaskContext.executando(
                TarefaIa.CLASSIFICACAO_RISCO,
                () -> toolLoopRunner.executar(
                        chatModel,
                        mensagens,
                        ocorrenciaTools.contextoCompleto(),
                        ToolLoopPolicy.ateChamarTodasAsTools(),
                        MAX_RODADAS_FATOS));

        return resultado.algumaToolExecutada()
                ? resultado.resultadosConcatenados("\n")
                : "(não foi possível levantar dados no sistema)";
    }

    private String descricaoDoProduto(Ocorrencia ocorrencia) {
        if (ocorrencia.produtoId() == null) {
            return "não informado";
        }
        return produtoRepository.findById(ocorrencia.produtoId())
                .map(produto -> produto.descricao())
                .orElse("não informado");
    }

    private String ultimoParecer(Long ocorrenciaId) {
        return analiseRepository.findByOcorrenciaId(ocorrenciaId).stream()
                .findFirst()
                .map(Analise::parecer)
                .orElse(null);
    }

    /** O modelo as vezes devolve score fora da faixa mesmo com o schema pedindo 0-100. */
    private Integer normalizarScore(Integer score) {
        return score == null ? 0 : Math.clamp(score, 0, 100);
    }
}
