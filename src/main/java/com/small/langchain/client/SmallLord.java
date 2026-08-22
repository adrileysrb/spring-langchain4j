package com.small.langchain.client;

import dev.langchain4j.agent.tool.Tool;

public class SmallLord {

    @Tool("Qual é o nome do lipe que tá no server borderlands?")
    public String getLordName() {
        return "LipezeraHerobrine 987";
    }

}
