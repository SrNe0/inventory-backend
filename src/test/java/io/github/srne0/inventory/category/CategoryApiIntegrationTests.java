package io.github.srne0.inventory.category;

import io.github.srne0.inventory.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de punta a punta: HTTP, validación, servicio, JPA y PostgreSQL real.
 * Sin @Transactional: cada petición confirma su propia transacción y la prueba ve
 * lo que realmente queda en la tabla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryApiIntegrationTests {

    private static final String BASE = "/api/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("ciclo completo: crear, leer por Location, actualizar, borrar y recibir 404")
    void fullLifecycle() throws Exception {
        MvcResult created = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Bebidas \",\"description\":\" Líquidos \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bebidas"))
                .andExpect(jsonPath("$.description").value("Líquidos"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        URI location = URI.create(created.getResponse().getHeader("Location"));
        assertThat(repository.count()).isEqualTo(1);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bebidas"));

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lácteos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lácteos"))
                .andExpect(jsonPath("$.description").doesNotExist());

        mockMvc.perform(get(location))
                .andExpect(jsonPath("$.name").value("Lácteos"));

        mockMvc.perform(delete(location)).andExpect(status().isNoContent());
        assertThat(repository.count()).isZero();

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("No existe una categoría con id")));
    }

    @Test
    @DisplayName("el nombre es único sin distinguir mayúsculas ni espacios sobrantes")
    void duplicateNameIsRejected() throws Exception {
        create("Bebidas").andExpect(status().isCreated());

        create("bebidas").andExpect(status().isConflict());
        create(" Bebidas ").andExpect(status().isConflict());
        create("BEBIDAS")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT permite cambiar solo las mayúsculas del propio nombre")
    void updateOwnNameChangingCase() throws Exception {
        long id = createAndGetId("Bebidas");

        mockMvc.perform(put(BASE + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"BEBIDAS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BEBIDAS"));
    }

    @Test
    @DisplayName("PUT con el nombre de otra categoría devuelve 409")
    void updateToAnotherCategoryName() throws Exception {
        createAndGetId("Bebidas");
        long otherId = createAndGetId("Lácteos");

        mockMvc.perform(put(BASE + "/" + otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bebidas\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un nombre en blanco devuelve 400 con el error del campo y no guarda nada")
    void validationErrorIsReported() throws Exception {
        create("   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("borrar o actualizar un id inexistente devuelve 404")
    void missingIdReturns404() throws Exception {
        mockMvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("25 categorías en páginas de 10: totales correctos y orden por nombre")
    void pagination() throws Exception {
        for (int i = 1; i <= 25; i++) {
            create(String.format("Categoría %02d", i)).andExpect(status().isCreated());
        }

        mockMvc.perform(get(BASE).param("size", "10").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Categoría 01"))
                .andExpect(jsonPath("$.content[9].name").value("Categoría 10"))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.page.number").value(0));

        mockMvc.perform(get(BASE).param("size", "10").param("page", "2"))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[4].name").value("Categoría 25"))
                .andExpect(jsonPath("$.page.number").value(2));

        mockMvc.perform(get(BASE).param("sort", "name,desc").param("size", "3"))
                .andExpect(jsonPath("$.content[0].name").value("Categoría 25"));
    }

    @Test
    @DisplayName("sin parámetros: 20 por página, y un size de 500 se recorta a 100")
    void defaultAndMaximumPageSize() throws Exception {
        for (int i = 1; i <= 25; i++) {
            create(String.format("Categoría %02d", i)).andExpect(status().isCreated());
        }

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page.size").value(20));

        mockMvc.perform(get(BASE).param("size", "500"))
                .andExpect(jsonPath("$.content.length()").value(25))
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    @DisplayName("con empates en createdAt el id desempata y ninguna fila se repite ni se salta")
    void tiesAreBrokenById() throws Exception {
        for (int i = 1; i <= 5; i++) {
            jdbc.update("INSERT INTO category (name, created_at, updated_at) VALUES (?, '2026-01-01T00:00:00Z', now())",
                    "Empate " + i);
        }

        for (String direction : List.of("asc", "desc")) {
            List<Integer> ids = new ArrayList<>();
            for (int page = 0; page < 3; page++) {
                MvcResult result = mockMvc.perform(get(BASE)
                                .param("sort", "createdAt," + direction)
                                .param("size", "2")
                                .param("page", String.valueOf(page)))
                        .andExpect(status().isOk())
                        .andReturn();
                ids.addAll(com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),
                        "$.content[*].id"));
            }
            assertThat(ids).as("ids recorridos con createdAt," + direction)
                    .hasSize(5).doesNotHaveDuplicates().isSorted();
        }
    }

    @Test
    @DisplayName("ordenar por un campo no permitido devuelve 400 con la lista de campos")
    void invalidSortReturns400() throws Exception {
        mockMvc.perform(get(BASE).param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("createdAt, id, name, updatedAt")));
    }

    private org.springframework.test.web.servlet.ResultActions create(String name) throws Exception {
        return mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"));
    }

    private long createAndGetId(String name) throws Exception {
        MvcResult result = create(name).andExpect(status().isCreated()).andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
