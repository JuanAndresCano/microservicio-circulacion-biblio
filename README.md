# microservicio-circulacion

Microservicio de Circulación de la biblioteca, asegurado con **Spring Security + OAuth2 Resource Server (JWT)** contra **Keycloak** (Taller 6).

- Puerto del servicio: `8083`
- Base de datos: H2 en memoria
- Emisor de tokens: Keycloak, realm `biblioteca`

---

## Requisitos

| Herramienta | Versión probada | Nota |
|-------------|-----------------|------|
| JDK | **17 o 21** | Ver "Nota sobre la versión de Java" más abajo. Con JDK 23+ hay que ajustar Lombok. |
| Maven | 3.9.x | |
| Docker | cualquiera reciente | Solo para levantar Keycloak |

---

## 1. Levantar Keycloak

Este taller asume Keycloak en `http://localhost:8080`. Para no chocar con otros
contenedores, se usa uno dedicado en modo desarrollo (BD H2 en memoria):

```bash
docker run -d --name biblioteca-keycloak -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.7.0 start-dev
```

Consola admin: `http://localhost:8080` → `admin` / `admin`.

### Configuración dentro de Keycloak

1. **Realm**: crear uno llamado `biblioteca`.
2. **Cliente**:
   - Client ID: `circulacion-service`
   - Client authentication: `On` (confidential)
   - Direct access grants: `On` (permite pedir token con usuario/contraseña)
   - Valid redirect URIs: `http://localhost:8083/*`
3. **Roles de realm**: `ROLE_LIBRARIAN`, `ROLE_USER`.
4. **Usuarios de prueba**: al menos uno con `ROLE_LIBRARIAN` y otro con `ROLE_USER`
   (pestaña *Credentials* → definir contraseña, *Temporary* = Off).
5. El **client secret** está en el cliente → pestaña *Credentials*. Se usa solo
   para pedir el token (Postman/curl), **no** va en `application.properties`.

---

## 2. Configuración del servicio

`src/main/resources/application.properties`:

```properties
server.port=8083
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/biblioteca
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/certs
```

> El `issuer-uri` debe coincidir **carácter por carácter** con el claim `iss` de los
> tokens. Si Keycloak corre en otro puerto, ajústalo aquí.

El servicio es un *resource server*: valida la firma del JWT contra `jwk-set-uri`.
No necesita el client secret.

---

## 3. Compilar y ejecutar

```bash
mvn clean spring-boot:run
```

Listo cuando aparece `Started CirculacionServiceApplication`.

---

## 4. Probar

Colección de Postman incluida en `postman/`:

- `Circulacion-Keycloak.postman_collection.json`
- `Circulacion-Keycloak.postman_environment.json`

Importar ambos, seleccionar el environment, rellenar `client_secret` y las
contraseñas, y correr la carpeta **0. Auth** primero (guarda los tokens en
variables), luego las demás.

Prueba rápida con curl:

```bash
# 1. endpoint público (sin token) -> 200
curl -i http://localhost:8083/circulacion/public/status

# 2. endpoint protegido sin token -> 401
curl -i http://localhost:8083/circulacion/prestamos

# 3. pedir token
TOKEN=$(curl -s http://localhost:8080/realms/biblioteca/protocol/openid-connect/token \
  -d grant_type=password -d client_id=circulacion-service \
  -d client_secret=TU_SECRET \
  -d username=librarian1 -d password=TU_PASSWORD | jq -r .access_token)

# 4. con token válido -> 200
curl -i http://localhost:8083/circulacion/prestamos -H "Authorization: Bearer $TOKEN"
```

### Endpoints

| Método | Ruta | Regla |
|--------|------|-------|
| GET  | `/circulacion/public/status` | público |
| GET  | `/circulacion/prestamos`     | `ROLE_LIBRARIAN` o `ROLE_USER` |
| POST | `/circulacion/prestar?usuarioId=&libroId=` | `ROLE_LIBRARIAN` |
| POST | `/circulacion/devolver?prestamoId=` | `ROLE_LIBRARIAN` |

---

## Nota sobre la versión de Java (léase si "no compila")

El proyecto declara `spring-boot-starter-parent` **3.3.2**, que fija **Lombok
1.18.32**. Esa versión de Lombok **no soporta JDK 23, 24 ni 25**: el procesador de
anotaciones falla en silencio y `mvn compile` revienta con decenas de errores del
tipo:

```
constructor XxxId cannot be applied to given types  (required: no arguments)
cannot find symbol: method getXxx()  / setXxx() / getXxx_value()
```

Son todos el mismo problema: Lombok no generó getters/setters/constructores.

### Solución aplicada (solo en este `pom.xml`, sin tocar el entorno)

Se sube Lombok a una versión compatible con JDK 25 y se declara explícitamente
como *annotation processor*:

```xml
<properties>
    <java.version>17</java.version>
    <lombok.version>1.18.48</lombok.version>   <!-- override del 1.18.32 del parent -->
</properties>
```

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
    <!-- ... spring-boot-maven-plugin ... -->
  </plugins>
</build>
```

Con esto compila en JDK 17, 21 y 25.

### Alternativa (sin tocar el `pom.xml`)

Usar JDK 17 o 21 para este proyecto. Con SDKMAN:

```bash
sdk install java 21.0.4-tem
sdk use java 21.0.4-tem        # solo en la sesión actual de la terminal
```

o dejar un archivo `.sdkmanrc` en la carpeta del proyecto:

```
java=21.0.4-tem
```

y ejecutar `sdk env` al entrar.

---

## Otros ajustes hechos durante el taller

- **`config/KeycloakRealmRoleConverter.java`**: el archivo debía llamarse igual que
  la clase pública que contiene (`KeycloakRealmRoleConverter`). Un nombre distinto
  aborta la compilación antes de que corra Lombok, lo que multiplica los errores.
- **`CirculacionController.java`**: faltaba el import
  `org.springframework.security.access.prepost.PreAuthorize`.
- **`application.properties`**: al copiar del PDF se partieron nombres de
  propiedades (`issueruri` → `issuer-uri`, `jwk-seturi` → `jwk-set-uri`,
  `openidconnect` → `openid-connect`). Spring exige el nombre exacto.
- Las propiedades `keycloak.*` (adaptador antiguo de Keycloak) ya no existen en
  Spring Boot 3 y se pueden eliminar.
