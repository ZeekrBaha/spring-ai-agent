package com.baha.agent.web;

import com.baha.agent.agent.AgentService;
import com.baha.agent.agent.ChatResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AgentService agentService;

    @Test
    void postReturnsReplyToolsAndConversationId() throws Exception {
        when(agentService.chat(anyString(), anyString()))
                .thenReturn(new ChatResult("4", List.of("calculate")));

        mvc.perform(post("/api/chat").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"what is 2+2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("4"))
                .andExpect(jsonPath("$.toolsUsed[0]").value("calculate"))
                .andExpect(jsonPath("$.conversationId").isNotEmpty());
    }

    @Test
    void blankMessageReturns400() throws Exception {
        mvc.perform(post("/api/chat").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
