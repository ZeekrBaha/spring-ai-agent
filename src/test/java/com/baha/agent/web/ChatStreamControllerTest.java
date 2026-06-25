package com.baha.agent.web;

import com.baha.agent.agent.AgentService;
import com.baha.agent.agent.ChatStreamEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatStreamController.class)
class ChatStreamControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AgentService agentService;

    @Test
    void streamsTokenEventsThenDoneAsServerSentEvents() throws Exception {
        when(agentService.chatStream(anyString(), anyString())).thenReturn(Flux.just(
                ChatStreamEvent.token("Hel"),
                ChatStreamEvent.token("lo"),
                ChatStreamEvent.done("Hello", List.of("calculate"), "c1")));

        MvcResult started = mvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("event:token");
        assertThat(body).contains("Hel");
        assertThat(body).contains("lo");
        assertThat(body).contains("event:done");
        assertThat(body).contains("calculate");
    }

    @Test
    void blankMessageReturns400() throws Exception {
        mvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
