package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTests {

    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService service;

    @Test
    @DisplayName("POST válido devuelve 201 con Location y el cuerpo creado")
    void createReturns201() throws Exception {
        when(service.create(any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(7L, "Bebidas", "Líquidos", NOW, NOW));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\",\"description\":\"Líquidos\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/categories/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Bebidas"));
    }

    @Test
    @DisplayName("POST con nombre en blanco devuelve 400 y no llega al servicio")
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST con nombre de más de 100 caracteres devuelve 400")
    void createWithLongNameReturns400() throws Exception {
        String longName = "a".repeat(101);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET por id devuelve 200")
    void getReturns200() throws Exception {
        when(service.get(7L)).thenReturn(new CategoryResponse(7L, "Bebidas", null, NOW, NOW));

        mockMvc.perform(get("/api/v1/categories/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bebidas"));
    }

    @Test
    @DisplayName("PUT válido devuelve 200")
    void updateReturns200() throws Exception {
        when(service.update(eq(7L), any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(7L, "Lácteos", null, NOW, NOW));

        mockMvc.perform(put("/api/v1/categories/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lácteos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lácteos"));
    }

    @Test
    @DisplayName("DELETE devuelve 204 sin cuerpo")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/7"))
                .andExpect(status().isNoContent());

        verify(service).delete(7L);
    }
}
