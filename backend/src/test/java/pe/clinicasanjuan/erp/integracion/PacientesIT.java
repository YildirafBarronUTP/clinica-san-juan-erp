package pe.clinicasanjuan.erp.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

class PacientesIT extends IntegracionBase {
    static final String NUEVO = """
            {"dni":"45678912","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"1990-05-01","sexo":"F",
             "telefono":"987654321","email":"ana@example.com"}""";

    @Test
    @DisplayName("CP-PAC-020 Registrar, consultar y buscar un paciente")
    void flujoBasico() {
        Integer id = como("admision").body(NUEVO).post("/pacientes")
                .then().statusCode(201)
                .body("historiaClinica", equalTo("HC-000301"))
                .body("estado", equalTo("ACTIVO"))
                .extract().path("id");
        como("medico").get("/pacientes/" + id).then().statusCode(200).body("dni", equalTo("45678912"));
        como("admision").queryParam("q", "4567891").get("/pacientes")
                .then().statusCode(200).body("totalElementos", equalTo(1));
    }

    @Test
    @DisplayName("CP-PAC-021 DNI duplicado devuelve 409 DNI_DUPLICADO")
    void duplicado() {
        como("admision").body(NUEVO.replace("45678912", "70000002")).post("/pacientes")
                .then().statusCode(409).body("codigo", equalTo("DNI_DUPLICADO")).body("campos[0].campo", equalTo("dni"));
    }

    @Test
    @DisplayName("CP-PAC-022 Validaciones de formato devuelven 400 con el campo exacto")
    void validaciones() {
        como("admision").body(NUEVO.replace("987654321", "12345")).post("/pacientes")
                .then().statusCode(400).body("codigo", equalTo("VALIDACION")).body("campos.campo", hasItem("telefono"));
        como("admision").body(NUEVO.replace("\"F\"", "\"X\"")).post("/pacientes")
                .then().statusCode(400).body("campos.campo", hasItem("sexo"));
        como("admision").body("{}").post("/pacientes")
                .then().statusCode(400).body("campos.campo", hasItems("dni", "nombres", "apellidos", "fechaNacimiento", "sexo"));
    }

    @Test
    @DisplayName("CP-PAC-023 Ciclo de estados: ACTIVO -> FALLECIDO es final")
    void estados() {
        como("admision").body("{\"estado\":\"FALLECIDO\"}").patch("/pacientes/3/estado")
                .then().statusCode(200).body("estadosPermitidos", empty());
        como("admision").body("{\"estado\":\"ACTIVO\"}").patch("/pacientes/3/estado")
                .then().statusCode(422).body("codigo", equalTo("TRANSICION_INVALIDA"));
    }

    @Test
    @DisplayName("CP-PAC-024 La semilla incluye al paciente alérgico a la penicilina")
    void semillaAlergia() {
        como("medico").get("/pacientes/1").then().statusCode(200)
                .body("dni", equalTo("70000001"))
                .body("alergias.principioActivo", hasItem("penicilina"));
        como("medico").body("{\"principioActivo\":\"PENICILINA\"}").post("/pacientes/1/alergias")
                .then().statusCode(409).body("codigo", equalTo("ALERGIA_DUPLICADA"));
    }

    @Test
    @DisplayName("CP-PAC-025 Búsqueda paginada y filtrada por estado")
    void busqueda() {
        como("admision").queryParam("estado", "INACTIVO").queryParam("size", 5).get("/pacientes")
                .then().statusCode(200)
                .body("totalElementos", equalTo(12)) // cada 25 pacientes de 300 está inactivo
                .body("contenido", hasSize(5))
                .body("contenido.estado", everyItem(equalTo("INACTIVO")));
        como("admision").get("/pacientes/99999").then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADO"));
    }
}
