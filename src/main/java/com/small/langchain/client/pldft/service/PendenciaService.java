package com.small.langchain.client.pldft.service;

import com.small.langchain.client.llm.model.ChatModelFactory;
import com.small.langchain.client.llm.observability.LlmTaskContext;
import com.small.langchain.client.llm.observability.TarefaIa;
import com.small.langchain.client.pldft.model.Analise;
import com.small.langchain.client.pldft.model.AnalisePendencia;
import com.small.langchain.client.pldft.model.PendenciaItem;
import com.small.langchain.client.pldft.model.PendenciasExtraidas;
import com.small.langchain.client.pldft.repository.AnalisePendenciaRepository;
import com.small.langchain.client.pldft.repository.AnaliseRepository;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PendenciaService {

    private final AnaliseRepository analiseRepository;
    private final AnalisePendenciaRepository pendenciaRepository;
    private final ChatModelFactory chatModelFactory;
    private final PendenciaExtractor extractor;

    public PendenciaService(
            AnaliseRepository analiseRepository,
            AnalisePendenciaRepository pendenciaRepository,
            ChatModelFactory chatModelFactory,
            PendenciaExtractor extractor
    ) {
        this.analiseRepository = analiseRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.chatModelFactory = chatModelFactory;
        this.extractor = extractor;
    }

    public List<AnalisePendencia> extrair(Long analiseId) {
        Analise analise = analiseRepository.findById(analiseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Análise não encontrada"));

        ChatModel chatModel = chatModelFactory.defaultModel();

        PendenciasExtraidas extraidas;
        try {
            extraidas = LlmTaskContext.executando(
                    TarefaIa.EXTRACAO_PENDENCIAS, () -> extractor.extrair(chatModel, analise));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao extrair pendências com a IA: " + e.getMessage(), e);
        }

        // Reextrair substitui o resultado anterior: a lista sempre reflete o parecer atual.
        pendenciaRepository.deleteByAnaliseId(analiseId);
        return extraidas.pendenciasOuVazio().stream()
                .filter(item -> item.descricao() != null && !item.descricao().isBlank())
                .map(item -> pendenciaRepository.save(new AnalisePendencia(
                        null, analiseId, item.descricao(),
                        valorValido(item.tipo(), PendenciaItem.TIPOS, "OUTRO"),
                        valorValido(item.prioridade(), PendenciaItem.PRIORIDADES, "MEDIA"),
                        LocalDateTime.now())))
                .toList();
    }

    public List<AnalisePendencia> listar(Long analiseId) {
        return pendenciaRepository.findByAnaliseId(analiseId);
    }

    /**
     * Mesmo com enum no schema, modelo pequeno as vezes devolve um valor proximo mas fora da lista;
     * como a coluna tem CHECK, vale normalizar antes de gravar.
     */
    private String valorValido(String valor, List<String> permitidos, String padrao) {
        if (valor == null) {
            return padrao;
        }
        String normalizado = valor.trim().toUpperCase();
        return permitidos.contains(normalizado) ? normalizado : padrao;
    }
}
