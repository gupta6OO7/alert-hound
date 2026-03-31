package com.incident.alerthound.logingestion.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.logingestion.service.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogService logService;

    @Test
    void shouldAcceptValidLogRequest() throws Exception {
        LogRequest request = new LogRequest(
                "payment-service",
                "ERROR",
                "DB connection timeout",
                "2026-03-31T10:00:00Z",
                "abc-123"
        );

        BDDMockito.given(logService.processLog(request)).willReturn("log-123");

        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.logId").value("log-123"));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        LogRequest request = new LogRequest(
                "",
                "ERROR",
                "",
                null,
                null
        );

        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
