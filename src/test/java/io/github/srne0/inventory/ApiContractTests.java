package io.github.srne0.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Protege el contrato: si cambia la API sin actualizar su documentación, o al revés,
 * estas pruebas lo detectan.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiContractTests {

    private static final String LIST = "$.paths['/api/v1/categories'].get";
    private static final String CREATE = "$.paths['/api/v1/categories'].post";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/v3/api-docs publica la información general de la API")
    void exposesInfo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de control de inventario"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.info.license.name").value("MIT"));
    }

    @Test
    @DisplayName("documenta todos los endpoints de categorías con sus métodos")
    void documentsEveryCategoryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/categories']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories/{id}'].delete").exists());
    }

    @Test
    @DisplayName("el listado expone page, size y sort como parámetros, no un objeto pageable")
    void listParametersAreFlattened() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(LIST + ".parameters[*].name", containsInAnyOrder("page", "size", "sort")));
    }

    @Test
    @DisplayName("el listado documenta el límite de tamaño y los campos de orden")
    void listDocumentsPaginationContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(LIST + ".description", containsString("máximo: 100")))
                .andExpect(jsonPath(LIST + ".description", containsString("id, name, createdAt, updatedAt")));
    }

    @Test
    @DisplayName("el POST documenta 201 con Location y los errores 400 y 409 como problem+json")
    void createDocumentsResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(CREATE + ".responses['201'].headers.Location").exists())
                .andExpect(jsonPath(CREATE + ".responses['201'].content['application/json']").exists())
                .andExpect(jsonPath(CREATE + ".responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath(CREATE + ".responses['409'].content['application/problem+json']").exists());
    }

    @Test
    @DisplayName("el esquema ProblemDetail describe el campo errors de las validaciones")
    void problemDetailDescribesErrors() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.errors.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.errors.items.properties.field")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.properties").doesNotExist());
    }

    @Test
    @DisplayName("el listado se documenta como PagedModel con content y page")
    void listResponseIsPagedModel() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.PagedModelCategoryResponse.properties.content").exists())
                .andExpect(jsonPath("$.components.schemas.PagedModelCategoryResponse.properties.page").exists());
    }

    @Test
    @DisplayName("Swagger UI responde")
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
