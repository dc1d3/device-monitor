package com.example.devicemonitor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет контракт API в мок-режиме — без телефона и без adb.
 */
@SpringBootTest(properties = "app.device.mock=true")
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsFullDeviceState() throws Exception {
        mockMvc.perform(get("/api/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.battery").isNumber())
                .andExpect(jsonPath("$.model").value("Pixel 7"))
                .andExpect(jsonPath("$.manufacturer").value("Google"))
                .andExpect(jsonPath("$.storage.totalGb").isNumber())
                .andExpect(jsonPath("$.storage.freeGb").isNumber())
                .andExpect(jsonPath("$.readAt").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void refreshReturnsSameShape() throws Exception {
        mockMvc.perform(post("/api/device/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void historyStartsWithCollectedPoints() throws Exception {
        mockMvc.perform(get("/api/device")).andExpect(status().isOk());

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points[0].battery").isNumber())
                .andExpect(jsonPath("$.points[0].at").exists());
    }

    @Test
    void screenReturnsPng() throws Exception {
        byte[] body = mockMvc.perform(get("/api/screen"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).hasSizeGreaterThan(100);
        assertThat(body[0] & 0xFF).isEqualTo(0x89);
        assertThat(new String(body, 1, 3, StandardCharsets.US_ASCII)).isEqualTo("PNG");
    }

    @Test
    void wrongMethodKeeps405AndReturnsApiError() throws Exception {
        mockMvc.perform(post("/api/device"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("Метод не поддерживается для этого адреса"))
                .andExpect(jsonPath("$.path").value("/api/device"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unknownPathReturns404AsJson() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/unknown"));
    }

    @Test
    void openApiDocumentIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Device Monitor API"))
                .andExpect(jsonPath("$.paths['/api/device']").exists());
    }
}
