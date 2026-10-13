package pe.clinicasanjuan.erp.pacientes.service;

import java.time.LocalDate;

/** Datos editables de un paciente (crear y actualizar). */
public record DatosPaciente(String dni, String nombres, String apellidos, LocalDate fechaNacimiento, String sexo,
                            String telefono, String email, String direccion) {}
