package io.github.srne0.inventory.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI inventoryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de control de inventario")
                .version("v1")
                .description("""
                        API REST de control de inventario. Los errores siguen el formato \
                        ProblemDetail (RFC 9457, application/problem+json) y los listados \
                        devuelven un PagedModel con los campos content y page.""")
                .license(new License().name("MIT").url("https://opensource.org/license/mit")));
    }

    /**
     * El esquema que springdoc deduce de ProblemDetail muestra un campo "properties",
     * pero en el JSON real las propiedades extra van en el primer nivel (por ejemplo
     * "errors"). Se describe a mano para que el contrato refleje la respuesta real.
     */
    @Bean
    OpenApiCustomizer problemDetailSchema() {
        return openApi -> openApi.getComponents().addSchemas("ProblemDetail", new ObjectSchema()
                .description("Error con formato RFC 9457 (application/problem+json)")
                .addProperty("type", new StringSchema().format("uri").example("about:blank"))
                .addProperty("title", new StringSchema().example("Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("No existe una categoría con id 99"))
                .addProperty("instance", new StringSchema().format("uri").example("/api/v1/categories/99"))
                .addProperty("errors", new ArraySchema()
                        .description("Solo en los 400 de validación: un elemento por campo inválido")
                        .items(new ObjectSchema()
                                .addProperty("field", new StringSchema().example("name"))
                                .addProperty("message", new StringSchema().example("El nombre es obligatorio")))));
    }
}
