package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import io.github.srne0.inventory.common.pagination.InvalidSortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @DisplayName("GET de la lista devuelve un PagedModel con content y page")
    void listReturnsPagedModel() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        when(service.list(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new CategoryResponse(1L, "Bebidas", null, NOW, NOW)), pageable, 41));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bebidas"))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(41))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    @DisplayName("sin parámetros usa tamaño 20 y orden por name ascendente")
    void listUsesDefaults() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    @DisplayName("un size mayor que 100 se recorta a 100")
    void listClampsSize() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/categories").param("size", "500")).andExpect(status().isOk());

        assertThat(capturePageable().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("sort=createdAt,desc llega al servicio como orden descendente")
    void listPassesSort() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/categories").param("sort", "createdAt,desc")).andExpect(status().isOk());

        assertThat(capturePageable().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("un orden no permitido devuelve 400 como ProblemDetail")
    void listInvalidSortReturns400() throws Exception {
        when(service.list(any(Pageable.class)))
                .thenThrow(new InvalidSortException("password", Set.of("id", "name")));

        mockMvc.perform(get("/api/v1/categories").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede ordenar por \"password\". Campos permitidos: id, name"));
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("DELETE devuelve 204 sin cuerpo")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/7"))
                .andExpect(status().isNoContent());

        verify(service).delete(7L);
    }

    @Test
    @DisplayName("GET de una categoría inexistente devuelve 404 como ProblemDetail")
    void getMissingReturns404() throws Exception {
        when(service.get(99L)).thenThrow(new CategoryNotFoundException(99L));

        mockMvc.perform(get("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("No existe una categoría con id 99"))
                .andExpect(jsonPath("$.instance").value("/api/v1/categories/99"));
    }

    @Test
    @DisplayName("POST con nombre duplicado devuelve 409")
    void createDuplicateReturns409() throws Exception {
        when(service.create(any(CategoryRequest.class)))
                .thenThrow(new DuplicateCategoryNameException("Bebidas"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe una categoría con el nombre \"Bebidas\""));
    }

    @Test
    @DisplayName("una violación de integridad devuelve 409 sin filtrar detalles internos")
    void integrityViolationReturns409WithoutLeaking() throws Exception {
        when(service.create(any(CategoryRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates uk_category_name_lower"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La operación viola una restricción de integridad"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uk_category_name_lower"))));
    }

    @Test
    @DisplayName("la validación devuelve 400 con los errores por campo")
    void validationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("La petición tiene campos inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("El nombre es obligatorio"));
    }

    @Test
    @DisplayName("un id que no es número devuelve 400")
    void invalidIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/categories/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("un JSON mal formado devuelve 400")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
