package co.analisys.biblioteca.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Reemplaza @EnableGlobalMethodSecurity
public class SecurityConfig {
 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // API REST stateless autenticada por JWT: no hay sesion de navegador ni
 // formularios, asi que no existe el vector de CSRF. Con la proteccion
 // activa (el default de Spring Security) todo POST/PUT/DELETE se rechaza
 // por falta de token CSRF, aunque el Bearer sea valido.
 .csrf(csrf -> csrf.disable())
 .authorizeHttpRequests(authz -> authz
 // "/error" es la ruta interna a la que Spring reenvia cuando una peticion
 // termina en error. Si no se permite, la seguridad intercepta ese reenvio y
 // responde 401, ocultando el error real (un 404, un 500 de otro servicio...).
 // Swagger UI y el JSON de OpenAPI son documentacion publica: sin esto
 // la seguridad los bloquea con 401 y la pagina ni siquiera carga.
 .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
 .requestMatchers("/circulacion/public/**", "/error").permitAll()
.anyRequest().authenticated())
 .oauth2ResourceServer(oauth2 -> oauth2
 .jwt(jwt ->
jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
 return http.build();
 }
 private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
 JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
 jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
 return jwtConverter;
 }
}