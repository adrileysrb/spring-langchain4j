package com.small.langchain.client;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.langchain4j.agent.tool.Tool;
import org.bson.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public class InsertTool {

    private static final List<String> WORDS = List.of(
            "abacaxi", "foguete", "montanha", "girassol", "tempestade", "quimera", "bussola", "vulcao"
    );

    private final MongoDBClient mongoDBClient;

    public InsertTool(MongoDBClient mongoDBClient) {
        this.mongoDBClient = mongoDBClient;
    }

    @Tool("Cria a collection 'palavras' no MongoDB e insere um documento com a data atual e uma palavra aleatoria")
    public String createPalavrasCollection() {
        MongoDatabase database = mongoDBClient.getInstance().getDatabase("sample_mflix");
        MongoCollection<Document> collection = database.getCollection("palavras");

        String randomWord = WORDS.get(new Random().nextInt(WORDS.size()));
        Document document = new Document("date", LocalDate.now().toString())
                .append("word", randomWord);

        collection.insertOne(document);
        return "Documento inserido na collection 'palavras': " + document.toJson();
    }
}
