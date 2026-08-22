package com.small.langchain.client.playground.dto;

import java.util.List;

public record ChatRequestDto(
        List<ChatMessageDto> messages
) {
}
