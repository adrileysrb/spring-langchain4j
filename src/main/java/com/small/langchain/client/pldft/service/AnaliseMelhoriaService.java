package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.model.ModelSpec;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.llm.tool.ToolLoopPolicy;
import com.small.langchain.client.llm.tool.ToolLoopResult;
import com.small.langchain.client.llm.tool.ToolLoopRunner;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.Produto;
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
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
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
    // Modelos locais pequenos as vezes "fingem" ter chamado uma tool escrevendo texto solto em vez
    // de emitir um tool_call de verdade, principalmente quando o prompt e longo (ex.: pedindo pra
    // reescrever o parecer inteiro AO MESMO TEMPO que decide usar as tools). Por isso a consulta de
    // enquadramento roda numa chamada curta e isolada, so com essa unica tarefa -- uma rodada por
    // tool (PEP, PEM, funcionario) -- e so depois o resultado (real, vindo do cadastro) e passado
    // como texto pronto pra chamada de redacao, que ai nao precisa lidar com tool-calling nenhum.
    private static final int MAX_RODADAS_ENQUADRAMENTO = 3;

    private final AnaliseRepository analiseRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final ProdutoRepository produtoRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AnaliseMelhoriaRepository melhoriaRepository;
    private final ChatModelFactory chatModelFactory;
    private final ToolLoopRunner toolLoopRunner;
    private final OcorrenciaTools ocorrenciaTools;

    public AnaliseMelhoriaService(
            AnaliseRepository analiseRepository,
            OcorrenciaRepository ocorrenciaRepository,
            ProdutoRepository produtoRepository,
            PromptTemplateRepository promptTemplateRepository,
            AnaliseMelhoriaRepository melhoriaRepository,
            ChatModelFactory chatModelFactory,
            ToolLoopRunner toolLoopRunner,
            OcorrenciaTools ocorrenciaTools
    ) {
        this.analiseRepository = analiseRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.produtoRepository = produtoRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.melhoriaRepository = melhoriaRepository;
        this.chatModelFactory = chatModelFactory;
        this.toolLoopRunner = toolLoopRunner;
        this.ocorrenciaTools = ocorrenciaTools;
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
            ChatModel chatModel = chatModelFactory.forSpec(
                    new ModelSpec(template.modelo(), template.temperature(), template.maxTokens()));

            String enquadramento = consultarEnquadramentoViaTools(chatModel, ocorrencia, analiseId);

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
            ChatResponse response = LlmTaskContext.executando(
                    TarefaIa.REVISAO_PARECER, () -> chatModel.chat(chatRequest));
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
