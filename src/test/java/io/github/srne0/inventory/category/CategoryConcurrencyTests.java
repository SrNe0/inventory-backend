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
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Prueba de concurrencia: varias peticiones simultáneas crean la MISMA categoría.
 *
 * Sin @Transactional en la clase: cada petición debe usar su propia transacción
 * real. Si la prueba fuera transaccional, todo ocurriría en una sola conexión y
 * no habría carrera posible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryConcurrencyTests {

    private static final int THREADS = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("peticiones simultáneas con el mismo nombre: un 201, el resto 409 y ningún 500")
    void concurrentCreatesOfTheSameName() throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statuses = new ArrayList<>(THREADS);

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            List<Future<Integer>> futures = new ArrayList<>(THREADS);
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return createCategory("Bebidas");
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).as("todos los hilos listos").isTrue();
            start.countDown();

            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(statuses)
                .hasSize(THREADS)
                .doesNotContain(500)
                .containsOnly(201, 409);
        assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo(THREADS - 1);
        assertThat(repository.count()).isEqualTo(1);
    }

    private int createCategory(String name) throws Exception {
        return mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getStatus();
    }
}
