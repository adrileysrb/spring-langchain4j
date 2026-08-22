package com.small.langchain.client.playground;

import com.small.langchain.client.LlmClient;
import com.small.langchain.client.playground.dto.ChatRequestDto;
import com.small.langchain.client.playground.dto.ChatResponseDto;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/playground")
public class PlaygroundController {

    private final LlmClient llmClient;

    public PlaygroundController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/chat")
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        List<ChatMessage> messages = request.messages().stream()
                .<ChatMessage>map(m -> "assistant".equals(m.role())
                        ? AiMessage.from(m.content())
                        : UserMessage.from(m.content()))
                .toList();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .build();

        ChatResponse response = llmClient.getInstance().chat(chatRequest);
        return new ChatResponseDto(response.aiMessage().text());
    }
}
