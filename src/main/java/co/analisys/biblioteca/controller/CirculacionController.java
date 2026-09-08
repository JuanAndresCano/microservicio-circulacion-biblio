package co.analisys.biblioteca.controller;

import co.analisys.biblioteca.model.LibroId;
import co.analisys.biblioteca.model.Prestamo;
import co.analisys.biblioteca.model.PrestamoId;
import co.analisys.biblioteca.model.UsuarioId;
import co.analisys.biblioteca.service.CirculacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/circulacion")
@Tag(name = "Circulacion", description = "Prestamos y devoluciones de libros")
public class CirculacionController {
    @Autowired
    private CirculacionService circulacionService;

    @PostMapping("/prestar")
    @PreAuthorize("hasRole('ROLE_LIBRARIAN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Prestar un libro",
            description = "Registra un prestamo. Antes de crearlo consulta a catalogo-service si el "
                    + "libro esta disponible, y despues lo marca como no disponible y notifica al "
                    + "usuario. Requiere rol ROLE_LIBRARIAN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestamo registrado"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es valido"),
            @ApiResponse(responseCode = "403", description = "Token valido pero sin ROLE_LIBRARIAN"),
            @ApiResponse(responseCode = "500", description = "El libro no existe o no esta disponible")
    })
    public void prestarLibro(
            @Parameter(description = "Id del usuario que recibe el libro", example = "U001")
            @RequestParam String usuarioId,
            @Parameter(description = "Id del libro en catalogo-service", example = "1")
            @RequestParam String libroId) {
        circulacionService.prestarLibro(new UsuarioId(usuarioId), new LibroId(libroId));
    }

    @PostMapping("/devolver")
    @PreAuthorize("hasRole('ROLE_LIBRARIAN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Devolver un libro",
            description = "Marca el prestamo como DEVUELTO, libera el libro en catalogo-service y "
                    + "notifica al usuario. Requiere rol ROLE_LIBRARIAN. El prestamoId es un UUID "
                    + "que se obtiene de GET /circulacion/prestamos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolucion registrada"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es valido"),
            @ApiResponse(responseCode = "403", description = "Token valido pero sin ROLE_LIBRARIAN"),
            @ApiResponse(responseCode = "500", description = "El prestamo no existe")
    })
    public void devolverLibro(
            @Parameter(description = "Id del prestamo a devolver (UUID)",
                    example = "5018aad0-b2f9-4411-9753-f96949d38685")
            @RequestParam String prestamoId) {
        circulacionService.devolverLibro(new PrestamoId(prestamoId));
    }

    @GetMapping("/prestamos")
    @PreAuthorize("hasAnyRole('ROLE_LIBRARIAN', 'ROLE_USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "Listar todos los prestamos",
            description = "Devuelve los prestamos registrados. Accesible con ROLE_LIBRARIAN o ROLE_USER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de prestamos"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es valido"),
            @ApiResponse(responseCode = "403", description = "El token no tiene ninguno de los dos roles")
    })
    public List<Prestamo> obtenerTodosPrestamos() {
        return circulacionService.obtenerTodosPrestamos();
    }

    @GetMapping("/public/status")
    @Operation(
            summary = "Estado del servicio (publico)",
            description = "Endpoint sin autenticacion, util para comprobar que el servicio responde.",
            security = {})
    @ApiResponse(responseCode = "200", description = "El servicio esta operativo")
    public String getPublicStatus() {
        return "El servicio de circulación está funcionando correctamente";
    }

}
