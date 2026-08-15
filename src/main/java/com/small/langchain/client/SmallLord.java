package com.small.langchain.client;

import dev.langchain4j.agent.tool.Tool;

public class SmallLord {

    @Tool("ttes")
    public String getLordName() {
        return "KI123123";
    }

}
