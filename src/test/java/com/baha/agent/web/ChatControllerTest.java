package com.baha.agent.web;

import com.baha.agent.agent.AgentService;
import com.baha.agent.agent.AgentUpstreamException;
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
    void upstreamModelFailureReturnsFriendly502() throws Exception {
        when(agentService.chat(anyString(), anyString()))
                .thenThrow(new AgentUpstreamException(new RuntimeException("openai 401")));

        mvc.perform(post("/api/chat").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void genuineBugSurfacesAs500NotMaskedAs502() throws Exception {
        // A real defect (e.g. NPE/ISE) must NOT be disguised as an upstream error.
        when(agentService.chat(anyString(), anyString()))
                .thenThrow(new IllegalStateException("internal bug"));

        mvc.perform(post("/api/chat").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void blankMessageReturns400() throws Exception {
        mvc.perform(post("/api/chat").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
