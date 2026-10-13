package pe.clinicasanjuan.erp.fixtures;

import pe.clinicasanjuan.erp.pacientes.service.DatosPaciente;

import java.time.LocalDate;

/** Test Data Builder: cada prueba cambia solo el dato que le interesa. */
public class PacienteBuilder {
    private String dni = "45678912";
    private String nombres = "Ana María";
    private String apellidos = "Pérez Quispe";
    private LocalDate fechaNacimiento = LocalDate.of(1990, 5, 1);
    private String sexo = "F";
    private String telefono = "987654321";
    private String email = "ana@example.com";
    private String direccion = "Av. Arequipa 123, Lima";

    public static PacienteBuilder unPaciente() {
        return new PacienteBuilder();
    }

    public PacienteBuilder conDni(String dni) { this.dni = dni; return this; }
    public PacienteBuilder conNombres(String nombres) { this.nombres = nombres; return this; }
    public PacienteBuilder conFechaNacimiento(LocalDate f) { this.fechaNacimiento = f; return this; }
    public PacienteBuilder conTelefono(String t) { this.telefono = t; return this; }

    public DatosPaciente build() {
        return new DatosPaciente(dni, nombres, apellidos, fechaNacimiento, sexo, telefono, email, direccion);
    }
}
