package pe.clinicasanjuan.erp.integracion;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/** IDs de la semilla: producto 1 = MED-001 (casos borde de vencimiento), 9 = MED-009 (stock = mínimo). */
class FarmaciaIT extends IntegracionBase {
    static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Lima"));

    @Test
    @DisplayName("CP-FAR-030 FEFO real: usa el lote que vence hoy, salta el vencido")
    void fefoConSemilla() {
        como("farmacia").body("{\"cantidad\":50,\"referencia\":\"IT\"}").post("/productos/1/salidas")
                .then().statusCode(200)
                .body("numeroLote", contains("L001-002", "L001-003"))
                .body("cantidad", contains(40, 10));
    }

    @Test
    @DisplayName("CP-FAR-031 El saldo del kardex coincide con el stock físico tras varios movimientos")
    void kardexCuadra() {
        var f = como("farmacia");
        f.body("{\"numeroLote\":\"IT-1\",\"fechaVencimiento\":\"" + HOY.plusDays(200) + "\",\"cantidad\":100,\"costoUnitario\":0.40}")
                .post("/productos/2/lotes").then().statusCode(201);
        como("farmacia").body("{\"cantidad\":30,\"referencia\":\"IT\"}").post("/productos/2/salidas").then().statusCode(200);

        List<Integer> saldos = como("farmacia").get("/productos/2/kardex").then().statusCode(200).extract().path("saldo");
        int stockFisico = como("farmacia").get("/productos/2").then().extract().path("stockFisico");
        assertThat(saldos.get(saldos.size() - 1)).isEqualTo(stockFisico);
    }

    @Test
    @DisplayName("CP-FAR-032 Ingreso de lote vencido o con cantidad 0 se rechaza")
    void ingresoInvalido() {
        como("farmacia").body("{\"numeroLote\":\"X\",\"fechaVencimiento\":\"" + HOY + "\",\"cantidad\":10,\"costoUnitario\":1}")
                .post("/productos/2/lotes").then().statusCode(422).body("codigo", equalTo("VENCIMIENTO_INVALIDO"));
        como("farmacia").body("{\"numeroLote\":\"X\",\"fechaVencimiento\":\"" + HOY.plusDays(30) + "\",\"cantidad\":0,\"costoUnitario\":1}")
                .post("/productos/2/lotes").then().statusCode(422).body("codigo", equalTo("CANTIDAD_INVALIDA"));
    }

    @Test
    @DisplayName("CP-FAR-033 Alertas de la semilla: stock igual al mínimo, sin stock, por vencer y vencido")
    void alertasSemilla() {
        como("farmacia").get("/inventario/alertas").then().statusCode(200)
                .body("bajoMinimo.codigo", hasItems("MED-009", "MED-015", "MED-026"))
                .body("vencidos.numeroLote", hasItem("L001-001"))
                .body("porVencer.numeroLote", hasItems("L001-003", "L035-001", "L035-002"));
    }

    @Test
    @DisplayName("CP-FAR-034 Producto: código duplicado 409 y precio 0 es inválido")
    void productos() {
        String body = "{\"codigo\":\"MED-001\",\"nombre\":\"X\",\"principioActivo\":\"x\",\"presentacion\":\"T\",\"precioVenta\":1,\"stockMinimo\":1}";
        como("farmacia").body(body).post("/productos").then().statusCode(409).body("codigo", equalTo("CODIGO_DUPLICADO"));
        como("farmacia").body(body.replace("MED-001", "MED-900").replace("\"precioVenta\":1", "\"precioVenta\":0"))
                .post("/productos").then().statusCode(400).body("campos.campo", hasItem("precioVenta"));
        como("farmacia").body(body.replace("MED-001", "MED-900")).post("/productos").then().statusCode(201);
    }

    /**
     * Concurrencia: 10 dispensaciones simultáneas de 5 unidades sobre un producto con 25.
     * Algunas fallan (409 por bloqueo optimista o 422 por stock), pero nunca se vende más
     * de lo que hay y el kardex sigue cuadrando.
     */
    @Test
    @DisplayName("CP-FAR-035 Dispensaciones concurrentes nunca dejan stock negativo")
    void concurrencia() throws Exception {
        como("farmacia").body("{\"codigo\":\"MED-950\",\"nombre\":\"Conc\",\"principioActivo\":\"c\",\"presentacion\":\"T\",\"precioVenta\":1,\"stockMinimo\":0}")
                .post("/productos").then().statusCode(201);
        int id = como("farmacia").queryParam("q", "MED-950").get("/productos").then().extract().path("contenido[0].id");
        como("farmacia").body("{\"numeroLote\":\"C1\",\"fechaVencimiento\":\"" + HOY.plusDays(100) + "\",\"cantidad\":25,\"costoUnitario\":1}")
                .post("/productos/" + id + "/lotes").then().statusCode(201);

        String token = token("farmacia");
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                Response r = io.restassured.RestAssured.given().auth().oauth2(token)
                        .contentType("application/json").body("{\"cantidad\":5,\"referencia\":\"conc\"}")
                        .post("/productos/" + id + "/salidas");
                return r.statusCode();
            }));
        }
        salida.countDown();
        int exitos = 0;
        for (Future<Integer> f : resultados) {
            int s = f.get(30, TimeUnit.SECONDS);
            assertThat(s).isIn(200, 409, 422);
            if (s == 200) exitos++;
        }
        pool.shutdown();

        int stock = como("farmacia").get("/productos/" + id).then().extract().path("stockFisico");
        assertThat(exitos).isBetween(1, 5);
        assertThat(stock).isEqualTo(25 - 5 * exitos).isGreaterThanOrEqualTo(0);
        List<Integer> saldos = como("farmacia").get("/productos/" + id + "/kardex").then().extract().path("saldo");
        assertThat(saldos.get(saldos.size() - 1)).isEqualTo(stock);
    }
}
