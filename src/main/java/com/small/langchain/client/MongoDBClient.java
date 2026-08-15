package com.small.langchain.client;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoDBClient {

    private MongoClient mongoClient;

    @Value("${MONGO_USERNAME}")
    private String username;

    @Value("${MONGO_PASSWORD}")
    private String password;

    public MongoClient getInstance() {
        if(mongoClient != null) return mongoClient;

        String uri = String.format("mongodb+srv://%s:%s@cluster0.rokknh9.mongodb.net/?appName=Cluster0", username, password);
        mongoClient = MongoClients.create(uri);
        return mongoClient;
    }
}
