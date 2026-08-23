package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.guardrail.GuardrailChain;
import com.small.langchain.client.llm.guardrail.rules.NaoVazioGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemMarkdownGuardrail;
import com.small.langchain.client.llm.guardrail.rules.SemRecusaGuardrail;
import com.small.langchain.client.llm.guardrail.rules.TamanhoMinimoGuardrail;
import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.model.ModelSpec;
import com.small.langchain.client.llm.model.StreamingChatModelFactory;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.stream.SseStreamingHandler;
import com.small.langchain.client.llm.tool.ToolLoopPolicy;
import com.small.langchain.client.llm.tool.ToolLoopResult;
import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.guardrail.MencionaEnquadramentoGuardrail;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.Produto;
import com.small.langchain.client.pldft.model.PromptTemplate;
import com.small.langchain.client.pldft.repository.AnaliseMelhoriaRepository;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.ProdutoRepository;
import com.small.langchain.client.pldft.repository.PromptTemplateRepository;
import com.small.langchain.client.pldft.tool.OcorrenciaTools;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnaliseMelhoriaService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseMelhoriaService.class);

    private static final Set<String> STATUS_PERMITIDOS = Set.of("ACEITO", "DESCARTADO");

    // Modelos locais pequenos as vezes "fingem" ter chamado uma tool escrevendo texto solto em vez
    // de emitir um tool_call de verdade, principalmente quando o prompt e longo (ex.: pedindo pra
    // reescrever o parecer inteiro AO MESMO TEMPO que decide usar as tools). Por isso a consulta de
    // enquadramento roda numa chamada curta e isolada, so com essa unica tarefa -- uma rodada por
    // tool (PEP, PEM, funcionario) -- e so depois o resultado (real, vindo do cadastro) e passado
    // como texto pronto pra chamada de redacao, que ai nao precisa lidar com tool-calling nenhum.
    private static final int MAX_RODADAS_ENQUADRAMENTO = 3;

    /** Um parecer revisado mais curto que isso quase sempre e resumo, nao reescrita. */
    private static final int TAMANHO_MINIMO_DO_PARECER = 250;

    /** Geracao longa em modelo local: o timeout do SSE precisa acompanhar o do modelo. */
    private static final Duration TIMEOUT_STREAM = Duration.ofMinutes(6);

    /**
     * Cadeia aplicada a saida da reescrita: as tres primeiras regras sao genericas, a ultima e
     * de dominio -- e justamente a que pega o erro mais caro deste fluxo.
     */
    private static final GuardrailChain GUARDRAILS = GuardrailChain.de(
            new NaoVazioGuardrail(),
            new SemRecusaGuardrail(),
            new TamanhoMinimoGuardrail(TAMANHO_MINIMO_DO_PARECER),
            new MencionaEnquadramentoGuardrail(),
            new SemMarkdownGuardrail());

    private final AnaliseRepository analiseRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final ProdutoRepository produtoRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AnaliseMelhoriaRepository melhoriaRepository;
    private final ChatModelFactory chatModelFactory;
    private final StreamingChatModelFactory streamingChatModelFactory;
    private final ToolLoopRunner toolLoopRunner;
    private final OcorrenciaTools ocorrenciaTools;

    public AnaliseMelhoriaService(
            AnaliseRepository analiseRepository,
            OcorrenciaRepository ocorrenciaRepository,
            ProdutoRepository produtoRepository,
            PromptTemplateRepository promptTemplateRepository,
            AnaliseMelhoriaRepository melhoriaRepository,
            ChatModelFactory chatModelFactory,
            StreamingChatModelFactory streamingChatModelFactory,
            ToolLoopRunner toolLoopRunner,
            OcorrenciaTools ocorrenciaTools
    ) {
        this.analiseRepository = analiseRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.produtoRepository = produtoRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.melhoriaRepository = melhoriaRepository;
        this.chatModelFactory = chatModelFactory;
        this.streamingChatModelFactory = streamingChatModelFactory;
        this.toolLoopRunner = toolLoopRunner;
        this.ocorrenciaTools = ocorrenciaTools;
    }

    /** Geracao bloqueante: valida tudo antes de qualquer coisa ser gravada ou exibida. */
    public AnaliseMelhoria gerar(Long analiseId) {
        ContextoDaRevisao contexto = carregarContexto(analiseId);
        LocalDateTime inicio = LocalDateTime.now();
        try {
            ChatModel chatModel = chatModelFactory.forSpec(contexto.spec());
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(montarMensagens(contexto, chatModel))
                    .build();

            ChatResponse response = LlmTaskContext.executando(
                    TarefaIa.REVISAO_PARECER, () -> chatModel.chat(chatRequest));

            // A cadeia substitui a checagem solta de "texto vazio": qualquer reprovacao bloqueante
            // cai no catch abaixo e e gravada como ERRO, com o motivo exato da recusa.
            return concluir(contexto, response.aiMessage().text(), inicio);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            String mensagemErro = registrarErro(contexto, e, inicio);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o modelo de IA: " + mensagemErro, e);
        }
    }

    /**
     * Mesma geracao, entregue token a token via SSE.
     *
     * <p>A consulta de enquadramento continua bloqueante e acontece antes de abrir o stream: tool
     * calling e streaming juntos complicam sem ganho aqui, e o usuario nao ganharia nada vendo
     * tokens de uma chamada que so emite tool_calls. O que ele espera ver e o parecer sendo escrito.
     */
    public SseEmitter gerarComStream(Long analiseId) {
        ContextoDaRevisao contexto = carregarContexto(analiseId);
        LocalDateTime inicio = LocalDateTime.now();
        SseEmitter emitter = new SseEmitter(TIMEOUT_STREAM.toMillis());

        try {
            ChatModel chatModel = chatModelFactory.forSpec(contexto.spec());
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(montarMensagens(contexto, chatModel))
                    .build();

            StreamingChatModel streamingModel = streamingChatModelFactory.forSpec(contexto.spec());
            SseStreamingHandler handler = new SseStreamingHandler(emitter, textoCompleto -> {
                try {
                    return concluir(contexto, textoCompleto, inicio);
                } catch (RuntimeException e) {
                    registrarErro(contexto, e, inicio);
                    throw e;
                }
            });

            LlmTaskContext.executando(TarefaIa.REVISAO_PARECER, () -> {
                streamingModel.chat(chatRequest, handler);
                return null;
            });
        } catch (Exception e) {
            String mensagemErro = registrarErro(contexto, e, inicio);
            emitter.completeWithError(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o modelo de IA: " + mensagemErro, e));
        }
        return emitter;
    }

    public List<AnaliseMelhoria> listar(Long analiseId) {
        return melhoriaRepository.findByAnaliseId(analiseId);
    }

    public AnaliseMelhoria atualizarStatus(Long analiseId, Long id, String status) {
        if (!STATUS_PERMITIDOS.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status inválido, use ACEITO ou DESCARTADO");
        }
        AnaliseMelhoria melhoria = melhoriaRepository.updateStatus(id, status)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        if (!melhoria.analiseId().equals(analiseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada");
        }
        return melhoria;
    }

    private ContextoDaRevisao carregarContexto(Long analiseId) {
        Analise analise = analiseRepository.findById(analiseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada"));

        Ocorrencia ocorrencia = ocorrenciaRepository.findById(analise.ocorrenciaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada"));

        if (ocorrencia.produtoId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ocorrência sem produto definido");
        }

        PromptTemplate template = promptTemplateRepository.findAtivoByProdutoId(ocorrencia.produtoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nenhum prompt cadastrado para esse produto"));

        Produto produto = produtoRepository.findById(ocorrencia.produtoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        return new ContextoDaRevisao(analise, ocorrencia, produto, template);
    }

    private List<ChatMessage> montarMensagens(ContextoDaRevisao contexto, ChatModel chatModel) {
        String enquadramento = consultarEnquadramentoViaTools(chatModel, contexto.ocorrencia(), contexto.analiseId());

        Map<String, Object> variaveis = new HashMap<>();
        variaveis.put("parecer", contexto.analise().parecer());
        variaveis.put("analista", contexto.analise().analista());
        variaveis.put("ocorrenciaId", contexto.ocorrencia().id());
        variaveis.put("produto", contexto.produto().descricao());
        variaveis.put("enquadramento", enquadramento != null
                ? enquadramento
                : "não foi possível consultar o cadastro no momento");

        PromptTemplate template = contexto.template();
        List<ChatMessage> mensagens = new ArrayList<>();
        if (template.promptSistema() != null && !template.promptSistema().isBlank()) {
            mensagens.add(dev.langchain4j.model.input.PromptTemplate.from(template.promptSistema())
                    .apply(variaveis).toSystemMessage());
        }
        mensagens.add(dev.langchain4j.model.input.PromptTemplate.from(template.promptUsuario())
                .apply(variaveis).toUserMessage());
        return mensagens;
    }

    /**
     * Chamada curta e isolada, so com a tarefa de consultar o enquadramento (PEP, PEM e
     * funcionario, cada um numa tool propria) -- separada da chamada de redacao (bem mais
     * longa) porque modelos locais pequenos costumam falhar em emitir tool_calls de verdade
     * quando tem que decidir isso ao mesmo tempo que escreve um texto longo. Retorna null se,
     * mesmo apos as rodadas, nenhuma tool foi de fato chamada -- nesse caso a chamada de
     * redacao segue sem a informacao de cadastro.
     */
    private String consultarEnquadramentoViaTools(ChatModel chatModel, Ocorrencia ocorrencia, Long analiseId) {
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
                        MAX_RODADAS_ENQUADRAMENTO));

        if (!resultado.algumaToolExecutada()) {
            log.warn("Analise {}: nenhuma tool de enquadramento foi chamada de verdade, seguindo sem a informacao", analiseId);
            return null;
        }
        return resultado.resultadosConcatenados(" | ");
    }

    private AnaliseMelhoria concluir(ContextoDaRevisao contexto, String sugestao, LocalDateTime inicio) {
        GUARDRAILS.exigir(sugestao);
        return melhoriaRepository.save(new AnaliseMelhoria(
                null, contexto.analiseId(), contexto.template().id(), contexto.analise().parecer(),
                sugestao, "GERADO", null, inicio, LocalDateTime.now()));
    }

    private String registrarErro(ContextoDaRevisao contexto, Exception e, LocalDateTime inicio) {
        String mensagemErro = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        melhoriaRepository.save(new AnaliseMelhoria(
                null, contexto.analiseId(), contexto.template().id(), contexto.analise().parecer(),
                null, "ERRO", mensagemErro, inicio, LocalDateTime.now()));
        return mensagemErro;
    }

    /** Tudo que os dois modos de geracao precisam carregar antes de falar com o modelo. */
    private record ContextoDaRevisao(
            Analise analise, Ocorrencia ocorrencia, Produto produto, PromptTemplate template) {

        Long analiseId() {
            return analise.id();
        }

        ModelSpec spec() {
            return new ModelSpec(template.modelo(), template.temperature(), template.maxTokens());
        }
    }
}
