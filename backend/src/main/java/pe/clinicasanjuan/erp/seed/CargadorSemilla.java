package pe.clinicasanjuan.erp.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.clinicasanjuan.erp.farmacia.service.CalculosInventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Datos sintéticos y reproducibles (semilla fija). Perfiles:
 * <ul>
 *   <li>dev / demo: 1 usuario por rol, 300 pacientes, ~40 productos con lotes y casos borde.</li>
 *   <li>perf: lo mismo con 10 000 pacientes y más lotes por producto.</li>
 * </ul>
 * Los casos borde están documentados en docs/datos-de-prueba.md y los usan los E2E.
 */
@Component
public class CargadorSemilla implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CargadorSemilla.class);

    /** Paciente fijo con alergia a la penicilina: DNI 70000001. */
    public static final String DNI_PACIENTE_ALERGICO = "70000001";

    private static final String[] NOMBRES = {"Lucía", "Mateo", "Valentina", "Santiago", "Camila", "Sebastián",
            "Sofía", "Diego", "Mariana", "Joaquín", "Daniela", "Gabriel", "Fernanda", "Adrián", "Rosa", "Luis",
            "Carmen", "Jorge", "Ana", "Miguel", "Isabel", "José", "Patricia", "Carlos", "Milagros", "Renzo"};
    private static final String[] APELLIDOS = {"Quispe", "Flores", "Sánchez", "Rodríguez", "García", "Huamán",
            "Mamani", "Chávez", "Torres", "Ramírez", "Vargas", "Castillo", "Mendoza", "Rojas", "Gutiérrez", "Díaz",
            "Espinoza", "Cruz", "Romero", "Salazar", "Paredes", "Ccahuana", "Barrón", "Sotomayor", "Anastacio"};
    private static final String[] DISTRITOS = {"Ate", "San Juan de Lurigancho", "Comas", "Surco", "Los Olivos",
            "Villa El Salvador", "Chorrillos", "La Molina", "Breña", "San Miguel"};

    /** código, nombre, principio activo, presentación, precio, stock mínimo */
    private static final Object[][] PRODUCTOS = {
            {"MED-001", "Paracetamol 500 mg", "paracetamol", "Tableta", "0.50", 200},
            {"MED-002", "Ibuprofeno 400 mg", "ibuprofeno", "Tableta", "0.80", 150},
            {"MED-003", "Amoxicilina 500 mg", "amoxicilina", "Cápsula", "1.20", 120},
            {"MED-004", "Penicilina G benzatínica 1 200 000 UI", "penicilina", "Ampolla", "12.00", 20},
            {"MED-005", "Omeprazol 20 mg", "omeprazol", "Cápsula", "0.90", 100},
            {"MED-006", "Metformina 850 mg", "metformina", "Tableta", "0.60", 150},
            {"MED-007", "Losartán 50 mg", "losartán", "Tableta", "0.70", 120},
            {"MED-008", "Enalapril 10 mg", "enalapril", "Tableta", "0.40", 100},
            {"MED-009", "Atorvastatina 20 mg", "atorvastatina", "Tableta", "1.50", 80},
            {"MED-010", "Loratadina 10 mg", "loratadina", "Tableta", "0.50", 80},
            {"MED-011", "Cetirizina 10 mg", "cetirizina", "Tableta", "0.60", 60},
            {"MED-012", "Azitromicina 500 mg", "azitromicina", "Tableta", "3.50", 40},
            {"MED-013", "Ciprofloxacino 500 mg", "ciprofloxacino", "Tableta", "1.80", 60},
            {"MED-014", "Clotrimazol 1% crema 20 g", "clotrimazol", "Tubo", "6.50", 20},
            {"MED-015", "Salbutamol 100 mcg inhalador", "salbutamol", "Inhalador", "18.00", 15},
            {"MED-016", "Prednisona 20 mg", "prednisona", "Tableta", "0.90", 50},
            {"MED-017", "Dexametasona 4 mg/mL", "dexametasona", "Ampolla", "3.00", 30},
            {"MED-018", "Diclofenaco 75 mg/3 mL", "diclofenaco", "Ampolla", "2.50", 40},
            {"MED-019", "Metamizol 1 g/2 mL", "metamizol", "Ampolla", "2.20", 40},
            {"MED-020", "Cloruro de sodio 0.9% 1 L", "cloruro de sodio", "Frasco", "7.50", 50},
            {"MED-021", "Dextrosa 5% 1 L", "dextrosa", "Frasco", "8.00", 30},
            {"MED-022", "Ranitidina 300 mg", "ranitidina", "Tableta", "0.70", 40},
            {"MED-023", "Sales de rehidratación oral", "electrolitos", "Sobre", "1.00", 100},
            {"MED-024", "Ácido fólico 0.5 mg", "ácido fólico", "Tableta", "0.20", 100},
            {"MED-025", "Sulfato ferroso 300 mg", "sulfato ferroso", "Tableta", "0.30", 100},
            {"MED-026", "Insulina NPH 100 UI/mL", "insulina", "Frasco", "35.00", 10},
            {"MED-027", "Ketorolaco 30 mg/mL", "ketorolaco", "Ampolla", "3.20", 30},
            {"MED-028", "Clorfenamina 4 mg", "clorfenamina", "Tableta", "0.30", 80},
            {"MED-029", "Nitrofurantoína 100 mg", "nitrofurantoína", "Cápsula", "1.10", 40},
            {"MED-030", "Fluconazol 150 mg", "fluconazol", "Cápsula", "2.80", 20},
            {"MED-031", "Ondansetrón 8 mg", "ondansetrón", "Tableta", "2.40", 20},
            {"MED-032", "Amlodipino 5 mg", "amlodipino", "Tableta", "0.50", 80},
            {"MED-033", "Levotiroxina 100 mcg", "levotiroxina", "Tableta", "0.90", 40},
            {"MED-034", "Gentamicina 80 mg/2 mL", "gentamicina", "Ampolla", "2.00", 20},
            {"MED-035", "Ceftriaxona 1 g", "ceftriaxona", "Vial", "9.50", 20},
            {"MED-036", "Tramadol 50 mg", "tramadol", "Cápsula", "1.40", 30},
            {"MED-037", "Hioscina 10 mg", "hioscina", "Tableta", "0.80", 40},
            {"MED-038", "Gasa estéril 10x10 cm", "material médico", "Paquete", "1.50", 100},
            {"MED-039", "Jeringa 5 mL", "material médico", "Unidad", "0.40", 200},
            {"MED-040", "Alcohol 70% 1 L", "alcohol etílico", "Frasco", "12.00", 10},
    };

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SemillaProperties props;
    private final Clock reloj;

    public CargadorSemilla(JdbcTemplate jdbc, PasswordEncoder encoder, SemillaProperties props, Clock reloj) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.props = props;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        if ("none".equalsIgnoreCase(props.perfil())) return;
        Integer usuarios = jdbc.queryForObject("select count(*) from usuario", Integer.class);
        if (usuarios != null && usuarios > 0) {
            log.info("Semilla omitida: la base ya tiene datos");
            return;
        }
        cargar(props.perfil());
    }

    @Transactional
    public void cargar(String perfil) {
        boolean perf = "perf".equalsIgnoreCase(perfil);
        Random rnd = new Random(props.semilla());
        LocalDate hoy = LocalDate.now(reloj);
        Instant ahora = reloj.instant();
        usuarios();
        pacientes(rnd, hoy, ahora, perf ? 10_000 : 300);
        farmacia(rnd, hoy, ahora, perf ? 12 : 3);
        log.info("Semilla '{}' cargada (fecha base {})", perfil, hoy);
    }

    private void usuarios() {
        String hash = encoder.encode(props.password());
        Object[][] us = {
                {"admin", "Administrador del Sistema", "ADMIN"},
                {"admision", "Rosa Admisión Pérez", "ADMISION"},
                {"medico", "Dr. Jorge Médico Salazar", "MEDICO"},
                {"farmacia", "Q.F. Ana Farmacia Rojas", "FARMACIA"},
                {"caja", "Luis Caja Mendoza", "CAJA"},
        };
        for (Object[] u : us) {
            jdbc.update("insert into usuario(username, password_hash, nombre_completo, rol) values (?,?,?,?)",
                    u[0], hash, u[1], u[2]);
        }
    }

    private void pacientes(Random rnd, LocalDate hoy, Instant ahora, int total) {
        List<Object[]> filas = new ArrayList<>(total);
        Timestamp ts = Timestamp.from(ahora);
        for (int i = 1; i <= total; i++) {
            String dni = String.valueOf(70_000_000 + i);
            String nombre = NOMBRES[rnd.nextInt(NOMBRES.length)];
            String ap = APELLIDOS[rnd.nextInt(APELLIDOS.length)] + " " + APELLIDOS[rnd.nextInt(APELLIDOS.length)];
            LocalDate nac = hoy.minusDays(365L + rnd.nextInt(365 * 85));
            String sexo = rnd.nextBoolean() ? "F" : "M";
            String tel = "9" + String.format("%08d", rnd.nextInt(100_000_000));
            String estado = i % 25 == 0 ? "INACTIVO" : "ACTIVO";
            filas.add(new Object[]{"HC-%06d".formatted(i), dni, nombre, ap, Date.valueOf(nac), sexo, tel,
                    "Av. Principal " + (100 + rnd.nextInt(900)) + ", " + DISTRITOS[rnd.nextInt(DISTRITOS.length)],
                    estado, ts, ts});
        }
        jdbc.batchUpdate("""
                insert into paciente(historia_clinica, dni, nombres, apellidos, fecha_nacimiento, sexo, telefono,
                                     direccion, estado, creado_en, actualizado_en)
                values (?,?,?,?,?,?,?,?,?,?,?)""", filas);
        jdbc.execute("select setval('historia_clinica_seq', " + total + ")");
        Long alergico = jdbc.queryForObject("select id from paciente where dni = ?", Long.class, DNI_PACIENTE_ALERGICO);
        jdbc.update("insert into alergia(paciente_id, principio_activo, reaccion) values (?,?,?)",
                alergico, "penicilina", "Urticaria generalizada");
    }

    private void farmacia(Random rnd, LocalDate hoy, Instant ahora, int lotesPorProducto) {
        Timestamp ts = Timestamp.from(ahora);
        for (Object[] p : PRODUCTOS) {
            String codigo = (String) p[0];
            int minimo = (Integer) p[5];
            BigDecimal precio = new BigDecimal((String) p[4]);
            Long productoId = jdbc.queryForObject("""
                    insert into producto(codigo, nombre, principio_activo, presentacion, precio_venta, stock_minimo)
                    values (?,?,?,?,?,?) returning id""", Long.class, codigo, p[1], p[2], p[3], precio, minimo);

            List<int[]> lotes = new ArrayList<>(); // [diasHastaVencimiento, cantidad]
            switch (codigo) {
                // Casos borde documentados (docs/datos-de-prueba.md)
                case "MED-001" -> { lotes.add(new int[]{-10, 30}); lotes.add(new int[]{0, 40}); lotes.add(new int[]{1, 60}); lotes.add(new int[]{400, 500}); }
                case "MED-009" -> lotes.add(new int[]{300, minimo});               // stock exactamente igual al mínimo
                case "MED-015" -> lotes.add(new int[]{200, 5});                    // bajo el mínimo
                case "MED-026" -> { }                                              // sin stock
                case "MED-035" -> { lotes.add(new int[]{15, 25}); lotes.add(new int[]{29, 10}); } // por vencer
                default -> {
                    for (int i = 0; i < lotesPorProducto; i++) {
                        lotes.add(new int[]{60 + rnd.nextInt(700), minimo + rnd.nextInt(minimo * 3 + 20)});
                    }
                }
            }
            int saldo = 0;
            BigDecimal costoProm = BigDecimal.ZERO;
            int n = 1;
            for (int[] l : lotes) {
                BigDecimal costo = precio.multiply(BigDecimal.valueOf(55 + rnd.nextInt(20)))
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                String numero = "L" + codigo.substring(4) + "-" + String.format("%03d", n++);
                Long loteId = jdbc.queryForObject("""
                        insert into lote(producto_id, numero_lote, fecha_vencimiento, cantidad_inicial, cantidad_actual,
                                         costo_unitario, fecha_ingreso)
                        values (?,?,?,?,?,?,?) returning id""", Long.class,
                        productoId, numero, Date.valueOf(hoy.plusDays(l[0])), l[1], l[1], costo, ts);
                costoProm = CalculosInventario.costoPromedio(saldo, costoProm, l[1], costo);
                saldo += l[1];
                jdbc.update("""
                        insert into movimiento_kardex(producto_id, lote_id, tipo, cantidad, saldo, costo_unitario,
                                                      referencia, usuario, fecha)
                        values (?,?,'ENTRADA',?,?,?,?,'semilla',?)""",
                        productoId, loteId, l[1], saldo, costo, "Carga inicial " + numero, ts);
            }
            jdbc.update("update producto set costo_promedio = ? where id = ?", costoProm, productoId);
        }
    }
}
