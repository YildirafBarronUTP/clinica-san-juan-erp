package pe.clinicasanjuan.erp.integracion;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static io.restassured.RestAssured.given;

/**
 * Base de las pruebas de integración: API real + PostgreSQL real.
 * <p>
 * Por defecto levanta PostgreSQL 16 con Testcontainers (requiere Docker, como en CI).
 * Si se define TEST_DB_URL (y opcionalmente TEST_DB_USER / TEST_DB_PASSWORD) usa esa base,
 * útil en máquinas sin Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.seed.perfil=none", "app.seed.password=Clinica2026"})
@ActiveProfiles("test")
public abstract class IntegracionBase {
    protected static final String PASSWORD = "Clinica2026";

    private static final String URL_EXTERNA = System.getenv("TEST_DB_URL");
    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (URL_EXTERNA == null) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        if (POSTGRES != null) {
            r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            r.add("spring.datasource.username", POSTGRES::getUsername);
            r.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            r.add("spring.datasource.url", () -> URL_EXTERNA);
            r.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USER", "csj"));
            r.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "csj"));
        }
    }

    @LocalServerPort
    int puerto;

    @BeforeEach
    void reiniciarDatos() {
        RestAssured.port = puerto;
        RestAssured.basePath = "/api/v1";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        given().post("/test-support/reset").then().statusCode(204);
    }

    protected static String token(String usuario) {
        return given().contentType(ContentType.JSON)
                .body("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, PASSWORD))
                .post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    protected static RequestSpecification como(String usuario) {
        return given().auth().oauth2(token(usuario)).contentType(ContentType.JSON);
    }
}
