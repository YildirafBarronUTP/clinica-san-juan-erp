package pe.clinicasanjuan.erp.integracion;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class SeguridadIT extends IntegracionBase {

    @Test
    @DisplayName("CP-SEG-020 Sin token: 401 con formato de error único")
    void sinToken() {
        given().get("/pacientes").then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("CP-SEG-021 Token inválido: 401")
    void tokenInvalido() {
        given().auth().oauth2("a.b.c").get("/pacientes").then().statusCode(401);
    }

    @Test
    @DisplayName("CP-SEG-022 Bloqueo tras 5 intentos fallidos persiste en la base")
    void bloqueo() {
        for (int i = 0; i < 5; i++) {
            given().contentType(ContentType.JSON).body("{\"username\":\"caja\",\"password\":\"mala\"}")
                    .post("/auth/login").then().statusCode(401).body("codigo", equalTo("CREDENCIALES_INVALIDAS"));
        }
        given().contentType(ContentType.JSON).body("{\"username\":\"caja\",\"password\":\"" + PASSWORD + "\"}")
                .post("/auth/login").then().statusCode(401).body("codigo", equalTo("USUARIO_BLOQUEADO"));
    }

    @Test
    @DisplayName("CP-SEG-028 CORS: el frontend autorizado pasa el preflight, otro origen no")
    void cors() {
        given().header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .options("/auth/login").then().statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:3000");
        given().header("Origin", "https://sitio-malicioso.example").header("Access-Control-Request-Method", "POST")
                .options("/auth/login").then().statusCode(403);
    }

    @Test
    @DisplayName("CP-SEG-023 /auth/me devuelve el usuario del token")
    void yo() {
        como("medico").get("/auth/me").then().statusCode(200).body("rol", equalTo("MEDICO"));
    }

    /** Matriz de control de acceso (OWASP A01): rol x endpoint. */
    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @DisplayName("CP-SEG-024 Matriz de permisos por rol")
    @CsvSource({
            "admin,     GET,  /usuarios,               200",
            "admision,  GET,  /usuarios,               403",
            "medico,    GET,  /usuarios,               403",
            "farmacia,  GET,  /pacientes,              403",
            "caja,      GET,  /pacientes,              200",
            "caja,      GET,  /pacientes/1,            403",
            "medico,    GET,  /pacientes/1,            200",
            "medico,    GET,  /inventario/alertas,     403",
            "farmacia,  GET,  /inventario/alertas,     200",
            "caja,      GET,  /inventario/valorizacion, 403",
            "admin,     GET,  /inventario/valorizacion, 200",
            "medico,    GET,  /productos,              200",
            "admision,  GET,  /productos,              403",
    })
    void matrizPermisos(String usuario, String metodo, String ruta, int esperado) {
        como(usuario).request(metodo, ruta).then().statusCode(esperado);
    }

    @Test
    @DisplayName("CP-SEG-025 Solo ADMIN crea usuarios; el nuevo usuario puede iniciar sesión")
    void crearUsuario() {
        como("admision").body("{\"username\":\"x.user\",\"password\":\"Clave1234\",\"nombreCompleto\":\"X\",\"rol\":\"CAJA\"}")
                .post("/usuarios").then().statusCode(403);
        como("admin").body("{\"username\":\"caja2\",\"password\":\"Clave1234\",\"nombreCompleto\":\"Caja Dos\",\"rol\":\"CAJA\"}")
                .post("/usuarios").then().statusCode(201).body("rol", equalTo("CAJA"));
        given().contentType(ContentType.JSON).body("{\"username\":\"caja2\",\"password\":\"Clave1234\"}")
                .post("/auth/login").then().statusCode(200);
    }

    @Test
    @DisplayName("CP-SEG-026 Usuario desactivado no puede entrar y ADMIN no se desactiva a sí mismo")
    void desactivar() {
        como("admin").body("{\"activo\":false}").patch("/usuarios/1/estado")
                .then().statusCode(422).body("codigo", equalTo("AUTO_DESACTIVACION"));
        como("admin").body("{\"activo\":false}").patch("/usuarios/5/estado").then().statusCode(200);
        given().contentType(ContentType.JSON).body("{\"username\":\"caja\",\"password\":\"" + PASSWORD + "\"}")
                .post("/auth/login").then().statusCode(401).body("codigo", equalTo("USUARIO_INACTIVO"));
    }

    @Test
    @DisplayName("CP-SEG-027 Cada escritura queda en la bitácora de auditoría")
    void auditoria() {
        como("farmacia").body("{\"cantidad\":1,\"referencia\":\"Auditoría\"}").post("/productos/2/salidas").then().statusCode(200);
        String hoy = LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();
        como("admin").queryParam("entidad", "Producto").queryParam("desde", hoy).queryParam("hasta", hoy)
                .get("/auditoria").then().statusCode(200)
                .body("contenido[0].accion", equalTo("DISPENSAR"))
                .body("contenido[0].usuario", equalTo("farmacia"));
        como("admin").queryParam("desde", hoy).queryParam("hasta", "2000-01-01")
                .get("/auditoria").then().statusCode(422).body("codigo", equalTo("RANGO_INVALIDO"));
    }
}
