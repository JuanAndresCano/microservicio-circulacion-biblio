package co.analisys.biblioteca.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion OpenAPI del microservicio de Circulacion.
 *
 * Ademas de describir la API, declara el esquema de seguridad "bearer-jwt", que
 * es lo que hace aparecer el boton "Authorize" en Swagger UI: pegando ahi el
 * access_token que emite Keycloak se pueden probar los endpoints protegidos
 * desde la propia pagina, sin Postman.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI circulacionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Microservicio de Circulacion — Biblioteca")
                        .version("1.0")
                        .description("""
                                Gestion de prestamos y devoluciones de la biblioteca.

                                La API esta asegurada con Spring Security como OAuth2 Resource Server:
                                valida tokens JWT emitidos por Keycloak (realm `biblioteca`) y autoriza
                                por rol de realm.

                                **Como probar los endpoints protegidos:**
                                1. Pedir un token a Keycloak con el flujo `password`:
                                   `POST http://localhost:8080/realms/biblioteca/protocol/openid-connect/token`
                                   con `grant_type=password`, `client_id=circulacion-service`,
                                   el `client_secret`, y el usuario/contrasena.
                                2. Pulsar **Authorize** aqui arriba y pegar el `access_token`.
                                3. Ejecutar los endpoints. El token dura 300 segundos.

                                Usuarios de prueba: `librarian1` (ROLE_LIBRARIAN) y `user1` (ROLE_USER).
                                """))
                .servers(List.of(new Server()
                        .url("http://localhost:8083")
                        .description("Entorno local")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token emitido por Keycloak (realm biblioteca)")));
        // Sin requisito global a proposito: cada endpoint protegido declara su
        // @SecurityRequirement, de modo que /circulacion/public/status queda
        // documentado como lo que es, un endpoint abierto y sin candado.
    }
}
