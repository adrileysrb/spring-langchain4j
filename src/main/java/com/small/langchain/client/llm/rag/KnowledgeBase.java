package com.small.langchain.client.llm.rag;

import com.small.langchain.client.llm.config.LlmProperties;
import com.small.langchain.client.llm.model.EmbeddingModelFactory;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade sobre o pipeline de RAG do langchain4j. Esconde as quatro etapas -- carregar documentos,
 * quebrar em trechos, gerar embeddings e indexar -- atras de uma unica operacao de busca.
 *
 * <p>A indexacao e preguicosa e proposital: o modelo de embeddings vive fora da aplicacao e pode
 * simplesmente nao estar carregado. Indexar na subida acoplaria o start da aplicacao a um serviço
 * externo opcional; adiando pro primeiro uso, quem nao usa RAG nem percebe que ele existe.
 *
 * <p>Aqui o indice e {@link InMemoryEmbeddingStore}, que basta para uma base pequena e estavel
 * como um conjunto de normativos. Trocar por pgvector, Elasticsearch ou Qdrant significa apenas
 * outra implementacao de {@link EmbeddingStore} -- o resto do codigo nao muda.
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    private static final String PADRAO_DE_BUSCA = "classpath*:normativos/*.txt";
    private static final String METADADO_FONTE = "fonte";

    private final LlmProperties properties;
    private final EmbeddingModelFactory embeddingModelFactory;

    private volatile EmbeddingStore<TextSegment> store;

    public KnowledgeBase(LlmProperties properties, EmbeddingModelFactory embeddingModelFactory) {
        this.properties = properties;
        this.embeddingModelFactory = embeddingModelFactory;
    }

    public List<TrechoRecuperado> buscar(String pergunta) {
        return buscar(pergunta, properties.ragMaxResults(), properties.ragMinScore());
    }

    public List<TrechoRecuperado> buscar(String pergunta, int maxResultados, double scoreMinimo) {
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(indice())
                .embeddingModel(embeddingModelFactory.getInstance())
                .maxResults(maxResultados)
                .minScore(scoreMinimo)
                .build();

        return retriever.retrieve(Query.from(pergunta)).stream()
                .map(this::converter)
                .toList();
    }

    /** Reindexa a base -- util depois de alterar os arquivos de normativo. */
    public synchronized void reindexar() {
        store = null;
        indice();
    }

    private EmbeddingStore<TextSegment> indice() {
        EmbeddingStore<TextSegment> local = store;
        if (local == null) {
            synchronized (this) {
                local = store;
                if (local == null) {
                    local = indexar();
                    store = local;
                }
            }
        }
        return local;
    }

    private EmbeddingStore<TextSegment> indexar() {
        List<Document> documentos = carregarNormativos();
        if (documentos.isEmpty()) {
            throw new KnowledgeBaseException("Nenhum normativo encontrado em " + PADRAO_DE_BUSCA);
        }

        EmbeddingStore<TextSegment> novoIndice = new InMemoryEmbeddingStore<>();
        try {
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(
                            properties.ragChunkSize(), properties.ragChunkOverlap()))
                    .embeddingModel(embeddingModelFactory.getInstance())
                    .embeddingStore(novoIndice)
                    .build()
                    .ingest(documentos);
        } catch (RuntimeException e) {
            throw new KnowledgeBaseException("Falha ao gerar embeddings dos normativos. Verifique se o "
                    + "modelo '" + properties.embeddingModel() + "' está carregado em "
                    + properties.baseUrl() + ". Causa: " + e.getMessage(), e);
        }

        log.info("Base de normativos indexada: {} documento(s)", documentos.size());
        return novoIndice;
    }

    private List<Document> carregarNormativos() {
        try {
            Resource[] recursos = new PathMatchingResourcePatternResolver().getResources(PADRAO_DE_BUSCA);
            List<Document> documentos = new ArrayList<>(recursos.length);
            for (Resource recurso : recursos) {
                String texto = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                // A fonte viaja no metadado e sobrevive ao split, entao cada trecho recuperado
                // sabe de qual arquivo veio.
                documentos.add(Document.from(texto, Metadata.from(METADADO_FONTE, recurso.getFilename())));
            }
            return documentos;
        } catch (IOException e) {
            throw new KnowledgeBaseException("Falha ao ler os normativos do classpath: " + e.getMessage(), e);
        }
    }

    private TrechoRecuperado converter(Content content) {
        TextSegment segmento = content.textSegment();
        Object score = content.metadata().get(ContentMetadata.SCORE);
        return new TrechoRecuperado(
                segmento.metadata().getString(METADADO_FONTE),
                segmento.text(),
                score instanceof Number numero ? numero.doubleValue() : null);
    }
}
