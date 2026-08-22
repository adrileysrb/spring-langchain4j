package com.small.langchain.client;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.TEXT_PLAIN_VALUE)
public class LlmController {

    @Autowired
    private LlmClient llmClient;

    @GetMapping("/small")
    public String foo() {
        List<ToolSpecification> toolSpecificationList = ToolSpecifications.toolSpecificationsFrom(SmallLord.class);
        UserMessage userMessage = UserMessage.from("Quem é o lord anapolis discord borderlands?");
        ChatRequest request = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecificationList)
                .build();

        ChatResponse response = llmClient.getInstance().chat(request);
        AiMessage aiMessage = response.aiMessage();

        if (aiMessage.hasToolExecutionRequests()) {
            ToolExecutionRequest toolExecutionRequest = aiMessage.toolExecutionRequests().get(0);
            String toolResult = new SmallLord().getLordName();
            ToolExecutionResultMessage toolResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, toolResult);

            ChatRequest followUpRequest = ChatRequest.builder()
                    .messages(userMessage, aiMessage, toolResultMessage)
                    .toolSpecifications(toolSpecificationList)
                    .build();
            response = llmClient.getInstance().chat(followUpRequest);
        }

        return response.aiMessage().text();
    }

    @GetMapping("/small-ai")
    public String fooAiService() {
        LordAssistant lordAssistant = AiServices.builder(LordAssistant.class)
                .chatModel(llmClient.getInstance())
                .tools(new SmallLord())
                .build();

        return lordAssistant.chat("Quem é lipe do borderlands?");
    }
}
