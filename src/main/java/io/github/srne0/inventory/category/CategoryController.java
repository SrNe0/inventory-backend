package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Categorías", description = "Alta, consulta, edición y baja de categorías de productos")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(
            summary = "Crear una categoría",
            description = "Registra una nueva categoría con nombre único (sin distinguir mayúsculas/minúsculas). "
                    + "Recorta los espacios iniciales y finales del nombre y la descripción antes de guardar."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Categoría creada; incluye la cabecera Location con la URI del nuevo recurso",
                    headers = @Header(
                            name = "Location",
                            description = "URI de la categoría recién creada",
                            schema = @Schema(type = "string", format = "uri")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o JSON mal formado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una categoría con ese nombre",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(
            summary = "Listar categorías paginadas",
            description = "Devuelve una página de categorías en formato PagedModel (content + page). "
                    + "Tamaño por defecto: 20; máximo: 100 (un size mayor se recorta en silencio a 100, "
                    + "por lo que el cliente debe consultar siempre page.size y page.totalElements). "
                    + "Orden por defecto: name ascendente. Campos permitidos en sort: id, name, createdAt, updatedAt."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de categorías obtenida correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campo de ordenamiento (sort) no permitido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    // @SortDefault (y no @PageableDefault, que fijaría size=10) para que el tamaño
    // por defecto salga de spring.data.web.pageable.default-page-size.
    @GetMapping
    public PagedModel<CategoryResponse> list(@ParameterObject @SortDefault("name") Pageable pageable) {
        return new PagedModel<>(service.list(pageable));
    }

    @Operation(summary = "Obtener una categoría", description = "Devuelve la categoría con el id indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "El id no es un número válido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una categoría con ese id",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(
            summary = "Reemplazar una categoría",
            description = "Reemplaza por completo el nombre y la descripción (PUT). Si se omite la descripción, "
                    + "la categoría queda sin descripción. El nombre debe seguir siendo único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o JSON mal formado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una categoría con ese id",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe otra categoría con ese nombre",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Eliminar una categoría", description = "Elimina la categoría con el id indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una categoría con ese id",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
