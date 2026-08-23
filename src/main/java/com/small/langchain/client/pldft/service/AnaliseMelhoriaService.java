package com.small.langchain.client.pldft.service;

import com.small.langchain.client.LlmClient;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.Produto;
import com.small.langchain.client.pldft.repository.AnaliseMelhoriaRepository;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.PessoaMonitoradaRepository;
import com.small.langchain.client.pldft.repository.ProdutoRepository;
import com.small.langchain.client.pldft.repository.PromptTemplateRepository;
import com.small.langchain.client.pldft.tool.PessoaEnquadramentoTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    // Modelos locais pequenos as vezes "fingem" ter chamado a tool escrevendo texto solto em vez
    // de emitir um tool_call de verdade, principalmente quando o prompt e longo (ex.: pedindo pra
    // reescrever o parecer inteiro AO MESMO TEMPO que decide usar a tool). Por isso a consulta de
    // enquadramento roda numa chamada curta e isolada, so com essa unica tarefa -- e so depois o
    // resultado (real, vindo do cadastro) e passado como texto pronto pra chamada de redacao,
    // que ai nao precisa lidar com tool-calling nenhum.
    private static final int MAX_TENTATIVAS_SEM_TOOL_REAL = 2;

    private final AnaliseRepository analiseRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final ProdutoRepository produtoRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AnaliseMelhoriaRepository melhoriaRepository;
    private final PessoaMonitoradaRepository pessoaMonitoradaRepository;
    private final LlmClient llmClient;

    public AnaliseMelhoriaService(
            AnaliseRepository analiseRepository,
            OcorrenciaRepository ocorrenciaRepository,
            ProdutoRepository produtoRepository,
            PromptTemplateRepository promptTemplateRepository,
            AnaliseMelhoriaRepository melhoriaRepository,
            PessoaMonitoradaRepository pessoaMonitoradaRepository,
            LlmClient llmClient
    ) {
        this.analiseRepository = analiseRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.produtoRepository = produtoRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.melhoriaRepository = melhoriaRepository;
        this.pessoaMonitoradaRepository = pessoaMonitoradaRepository;
        this.llmClient = llmClient;
    }

    public AnaliseMelhoria gerar(Long analiseId) {
        Analise analise = analiseRepository.findById(analiseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada"));

        Ocorrencia ocorrencia = ocorrenciaRepository.findById(analise.ocorrenciaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada"));

        if (ocorrencia.produtoId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ocorrência sem produto definido");
        }

        com.small.langchain.client.pldft.model.PromptTemplate template = promptTemplateRepository
                .findAtivoByProdutoId(ocorrencia.produtoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nenhum prompt cadastrado para esse produto"));

        Produto produto = produtoRepository.findById(ocorrencia.produtoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        LocalDateTime inicio = LocalDateTime.now();
        try {
            OpenAiChatModel chatModel = llmClient.build(template.modelo(), template.temperature(), template.maxTokens());
            PessoaEnquadramentoTools tools = new PessoaEnquadramentoTools(ocorrenciaRepository, pessoaMonitoradaRepository);
            List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(tools);

            String enquadramento = consultarEnquadramentoViaTool(chatModel, tools, toolSpecifications, ocorrencia, analiseId);

            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("parecer", analise.parecer());
            variaveis.put("analista", analise.analista());
            variaveis.put("ocorrenciaId", ocorrencia.id());
            variaveis.put("produto", produto.descricao());
            variaveis.put("enquadramento", enquadramento != null
                    ? enquadramento
                    : "não foi possível consultar o cadastro no momento");

            List<ChatMessage> mensagens = new ArrayList<>();
            if (template.promptSistema() != null && !template.promptSistema().isBlank()) {
                mensagens.add(dev.langchain4j.model.input.PromptTemplate.from(template.promptSistema())
                        .apply(variaveis).toSystemMessage());
            }
            mensagens.add(dev.langchain4j.model.input.PromptTemplate.from(template.promptUsuario())
                    .apply(variaveis).toUserMessage());

            ChatRequest chatRequest = ChatRequest.builder().messages(mensagens).build();
            ChatResponse response = chatModel.chat(chatRequest);
            String sugestao = response.aiMessage().text();

            if (sugestao == null || sugestao.isBlank()) {
                throw new IllegalStateException(
                        "O modelo não retornou texto (resposta vazia, possivelmente truncada por max_tokens)");
            }

            return melhoriaRepository.save(new AnaliseMelhoria(
                    null, analiseId, template.id(), analise.parecer(), sugestao,
                    "GERADO", null, inicio, LocalDateTime.now()
            ));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            String mensagemErro = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            melhoriaRepository.save(new AnaliseMelhoria(
                    null, analiseId, template.id(), analise.parecer(), null,
                    "ERRO", mensagemErro, inicio, LocalDateTime.now()
            ));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o modelo de IA: " + mensagemErro, e);
        }
    }

    /**
     * Chamada curta e isolada, so com a tarefa de consultar o enquadramento via tool -- separada
     * da chamada de redacao (que e bem mais longa) porque modelos locais pequenos costumam falhar
     * em emitir um tool_call de verdade quando tem que decidir isso ao mesmo tempo que escreve um
     * texto longo. Retorna null se, mesmo com toolChoice=REQUIRED e as tentativas, o modelo nunca
     * chamar a tool de verdade -- nesse caso a chamada de redacao segue sem a informacao.
     */
    private String consultarEnquadramentoViaTool(
            OpenAiChatModel chatModel,
            PessoaEnquadramentoTools tools,
            List<ToolSpecification> toolSpecifications,
            Ocorrencia ocorrencia,
            Long analiseId
    ) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS_SEM_TOOL_REAL; tentativa++) {
            List<ChatMessage> mensagens = List.of(
                    SystemMessage.from("Voce verifica o enquadramento (PEP, PEM e/ou funcionario da instituicao) "
                            + "da pessoa monitorada de uma ocorrencia de PLDFT. Use a ferramenta disponivel "
                            + "informando o id da ocorrencia para consultar o cadastro. Nao escreva nenhum "
                            + "texto de resposta, apenas use a ferramenta."),
                    UserMessage.from("Consulte o enquadramento da pessoa monitorada da ocorrencia #" + ocorrencia.id() + ".")
            );

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(mensagens)
                    .toolSpecifications(toolSpecifications)
                    .toolChoice(ToolChoice.REQUIRED)
                    .build();
            ChatResponse response = chatModel.chat(chatRequest);

            if (response.aiMessage().hasToolExecutionRequests()) {
                ToolExecutionRequest toolExecutionRequest = response.aiMessage().toolExecutionRequests().get(0);
                log.info("Analise {}: chamando tool '{}' com args {}", analiseId,
                        toolExecutionRequest.name(), toolExecutionRequest.arguments());
                String resultado = new DefaultToolExecutor(tools, toolExecutionRequest).execute(toolExecutionRequest, null);
                log.info("Analise {}: resultado da tool '{}': {}", analiseId, toolExecutionRequest.name(), resultado);
                return resultado;
            }

            log.warn("Analise {}: tentativa {} de consultar enquadramento nao chamou a tool de verdade"
                    + (tentativa < MAX_TENTATIVAS_SEM_TOOL_REAL ? ", tentando de novo" : ", seguindo sem a informacao"),
                    analiseId, tentativa);
        }
        return null;
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
}
