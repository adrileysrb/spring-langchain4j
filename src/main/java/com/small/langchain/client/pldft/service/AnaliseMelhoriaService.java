package com.small.langchain.client.pldft.service;

import com.small.langchain.client.LlmClient;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.AnaliseMelhoria;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.model.Produto;
import com.small.langchain.client.pldft.repository.AnaliseMelhoriaRepository;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import com.small.langchain.client.pldft.repository.ProdutoRepository;
import com.small.langchain.client.pldft.repository.PromptTemplateRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnaliseMelhoriaService {

    private static final Set<String> STATUS_PERMITIDOS = Set.of("ACEITO", "DESCARTADO");

    private final AnaliseRepository analiseRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final ProdutoRepository produtoRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AnaliseMelhoriaRepository melhoriaRepository;
    private final LlmClient llmClient;

    public AnaliseMelhoriaService(
            AnaliseRepository analiseRepository,
            OcorrenciaRepository ocorrenciaRepository,
            ProdutoRepository produtoRepository,
            PromptTemplateRepository promptTemplateRepository,
            AnaliseMelhoriaRepository melhoriaRepository,
            LlmClient llmClient
    ) {
        this.analiseRepository = analiseRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.produtoRepository = produtoRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.melhoriaRepository = melhoriaRepository;
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

        Map<String, Object> variaveis = Map.of(
                "parecer", analise.parecer(),
                "analista", analise.analista(),
                "ocorrenciaId", ocorrencia.id(),
                "produto", produto.descricao()
        );

        LocalDateTime inicio = LocalDateTime.now();
        try {
            OpenAiChatModel chatModel = llmClient.build(template.modelo(), template.temperature(), template.maxTokens());

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
