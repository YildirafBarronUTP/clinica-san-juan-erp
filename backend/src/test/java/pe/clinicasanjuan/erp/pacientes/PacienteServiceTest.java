package pe.clinicasanjuan.erp.pacientes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;
import pe.clinicasanjuan.erp.fixtures.Relojes;
import pe.clinicasanjuan.erp.pacientes.model.EstadoPaciente;
import pe.clinicasanjuan.erp.pacientes.model.Paciente;
import pe.clinicasanjuan.erp.pacientes.repository.PacienteRepository;
import pe.clinicasanjuan.erp.pacientes.service.PacienteService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pe.clinicasanjuan.erp.fixtures.PacienteBuilder.unPaciente;
import static pe.clinicasanjuan.erp.fixtures.Relojes.HOY;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {
    @Mock PacienteRepository repo;
    @Mock AuditoriaService auditoria;
    PacienteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PacienteService(repo, auditoria, Relojes.fijo());
    }

    private Paciente existente(EstadoPaciente estado) {
        Paciente p = new Paciente("HC-000001", "45678912", "Ana", "Pérez", HOY.minusYears(30), "F", Instant.now());
        p.setEstado(estado);
        return p;
    }

    @Nested
    @DisplayName("Registrar")
    class Registrar {
        @Test
        @DisplayName("CP-PAC-003 Registra, normaliza espacios y genera historia clínica")
        void registraYGeneraHistoria() {
            when(repo.existsByDni("45678912")).thenReturn(false);
            when(repo.siguienteHistoria()).thenReturn(42L);
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

            Paciente p = servicio.registrar(unPaciente().conNombres("  Ana   María ").conTelefono("").build());

            assertThat(p.getHistoriaClinica()).isEqualTo("HC-000042");
            assertThat(p.getNombres()).isEqualTo("Ana María");
            assertThat(p.getTelefono()).isNull();
            assertThat(p.getEstado()).isEqualTo(EstadoPaciente.ACTIVO);
            verify(auditoria).registrar(eq("CREAR"), eq("Paciente"), any(), eq("HC-000042"));
        }

        @Test
        @DisplayName("CP-PAC-004 Rechaza DNI duplicado con 409")
        void dniDuplicado() {
            when(repo.existsByDni("45678912")).thenReturn(true);
            assertThatThrownBy(() -> servicio.registrar(unPaciente().build()))
                    .isInstanceOf(ConflictoException.class)
                    .extracting("codigo").isEqualTo("DNI_DUPLICADO");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("CP-PAC-005 Rechaza DNI con formato inválido antes de consultar la base")
        void dniInvalido() {
            assertThatThrownBy(() -> servicio.registrar(unPaciente().conDni("1234567").build()))
                    .extracting("codigo").isEqualTo("DNI_INVALIDO");
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("CP-PAC-006 Valor límite: nacido hoy es válido, mañana no")
        void fechaNacimientoLimiteSuperior() {
            when(repo.existsByDni(any())).thenReturn(false);
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
            assertThatCode(() -> servicio.registrar(unPaciente().conFechaNacimiento(HOY).build())).doesNotThrowAnyException();

            assertThatThrownBy(() -> servicio.registrar(unPaciente().conFechaNacimiento(HOY.plusDays(1)).build()))
                    .extracting("codigo").isEqualTo("FECHA_NACIMIENTO_FUTURA");
        }

        @Test
        @DisplayName("CP-PAC-007 Valor límite: 120 años exactos es válido, un día más no")
        void fechaNacimientoLimiteInferior() {
            when(repo.existsByDni(any())).thenReturn(false);
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
            assertThatCode(() -> servicio.registrar(unPaciente().conFechaNacimiento(HOY.minusYears(120)).build()))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> servicio.registrar(unPaciente().conFechaNacimiento(HOY.minusYears(120).minusDays(1)).build()))
                    .extracting("codigo").isEqualTo("FECHA_NACIMIENTO_INVALIDA");
        }
    }

    @Nested
    @DisplayName("Actualizar")
    class Actualizar {
        @Test
        @DisplayName("CP-PAC-008 No permite cambiar el DNI a uno de otro paciente")
        void dniDeOtro() {
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(existente(EstadoPaciente.ACTIVO)));
            when(repo.existsByDniAndIdNot("11111111", 1L)).thenReturn(true);
            assertThatThrownBy(() -> servicio.actualizar(1L, unPaciente().conDni("11111111").build()))
                    .isInstanceOf(ConflictoException.class);
        }

        @Test
        @DisplayName("CP-PAC-009 El expediente de un paciente fallecido es de solo lectura")
        void fallecidoSoloLectura() {
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(existente(EstadoPaciente.FALLECIDO)));
            assertThatThrownBy(() -> servicio.actualizar(1L, unPaciente().build()))
                    .extracting("codigo").isEqualTo("PACIENTE_FALLECIDO");
        }

        @Test
        @DisplayName("CP-PAC-011 Actualiza datos y audita el cambio de DNI")
        void actualizaYAudita() {
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(existente(EstadoPaciente.ACTIVO)));
            when(repo.existsByDniAndIdNot("22222222", 1L)).thenReturn(false);
            Paciente p = servicio.actualizar(1L, unPaciente().conDni("22222222").build());
            assertThat(p.getDni()).isEqualTo("22222222");
            verify(auditoria).registrar(eq("ACTUALIZAR"), eq("Paciente"), any(), contains("45678912 -> 22222222"));
        }
    }

    @Nested
    @DisplayName("Estados y alergias")
    class EstadosYAlergias {
        @Test
        @DisplayName("CP-PAC-012 Transición inválida devuelve TRANSICION_INVALIDA")
        void transicionInvalida() {
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(existente(EstadoPaciente.FALLECIDO)));
            assertThatThrownBy(() -> servicio.cambiarEstado(1L, EstadoPaciente.ACTIVO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
        }

        @Test
        @DisplayName("CP-PAC-013 Transición válida cambia el estado")
        void transicionValida() {
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(existente(EstadoPaciente.ACTIVO)));
            assertThat(servicio.cambiarEstado(1L, EstadoPaciente.INACTIVO).getEstado()).isEqualTo(EstadoPaciente.INACTIVO);
        }

        @Test
        @DisplayName("CP-PAC-014 Alergia duplicada sin importar mayúsculas ni espacios")
        void alergiaDuplicada() {
            Paciente p = existente(EstadoPaciente.ACTIVO);
            p.agregarAlergia("Penicilina", "urticaria");
            when(repo.findConAlergiasById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> servicio.agregarAlergia(1L, " penicilina ", null))
                    .extracting("codigo").isEqualTo("ALERGIA_DUPLICADA");
            assertThat(servicio.agregarAlergia(1L, "Ibuprofeno", null).getPrincipioActivo()).isEqualTo("Ibuprofeno");
        }

        @Test
        @DisplayName("CP-PAC-015 Paciente inexistente devuelve NoEncontrado")
        void inexistente() {
            when(repo.findConAlergiasById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(NoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("CP-PAC-016 La búsqueda limita el tamaño de página a 100 y recorta el filtro")
    void busquedaLimitaPagina() {
        Page<Paciente> vacia = new PageImpl<>(List.of());
        when(repo.buscar(eq("quispe"), isNull(), any(Pageable.class))).thenReturn(vacia);
        servicio.buscar("  quispe ", null, -1, 500);
        verify(repo).buscar(eq("quispe"), isNull(), argThat((Pageable pg) -> pg.getPageSize() == 100 && pg.getPageNumber() == 0));
    }
}
