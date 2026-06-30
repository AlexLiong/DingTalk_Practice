package com.example.dingtalk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {
    // 内存会话仓库，多用户隔离
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // 带滑动窗口记忆：只保留最近10条消息（5轮问答，防上下文溢出）
    @Bean
    public MessageWindowChatMemory chatMemory(InMemoryChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10) // 控制历史长度
                .build();
    }

    // 标准多轮记忆Advisor
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(MessageWindowChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
    @Bean
    public ChatClient chatClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, MessageChatMemoryAdvisor memoryAdvisor) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new RuntimeException("未检测到ChatClient.Builder，请检查大模型Starter依赖与配置");
        }
        return builder
                .defaultAdvisors(memoryAdvisor)
                .build();
    }
}
