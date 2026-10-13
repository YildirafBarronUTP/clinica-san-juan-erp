package pe.clinicasanjuan.erp.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(NoEncontradoException.class)
    ResponseEntity<ApiError> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, ApiError.of(404, "NO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(AutenticacionException.class)
    ResponseEntity<ApiError> autenticacion(AutenticacionException e) {
        return responder(HttpStatus.UNAUTHORIZED, ApiError.of(401, e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ApiError> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, ApiError.of(409, e.getCodigo(), e.getMessage(), e.getCampo()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ApiError.of(422, e.getCodigo(), e.getMessage(), e.getCampo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e) {
        List<ApiError.CampoError> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.CampoError(f.getField(), f.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST,
                new ApiError(400, "VALIDACION", "Datos inválidos", campos, Instant.now()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> malFormado(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, ApiError.of(400, "SOLICITUD_INVALIDA", "Cuerpo o parámetros mal formados"));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrencia(ObjectOptimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, ApiError.of(409, "MODIFICADO_EN_PARALELO",
                "Otro usuario modificó este registro; vuelva a intentarlo"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integridad(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, ApiError.of(409, "INTEGRIDAD", "El registro viola una restricción de datos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> prohibido(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, ApiError.of(403, "PROHIBIDO", "Su rol no tiene acceso a este recurso"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.of(500, "ERROR_INTERNO", "Error interno del servidor"));
    }

    private ResponseEntity<ApiError> responder(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status).body(body);
    }
}
