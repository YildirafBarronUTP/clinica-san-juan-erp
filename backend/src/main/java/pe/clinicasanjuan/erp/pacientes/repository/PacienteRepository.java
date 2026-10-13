package pe.clinicasanjuan.erp.pacientes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.clinicasanjuan.erp.pacientes.model.EstadoPaciente;
import pe.clinicasanjuan.erp.pacientes.model.Paciente;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    /** Carga el expediente con sus alergias (la API lo serializa fuera de la transacción). */
    @EntityGraph(attributePaths = "alergias")
    Optional<Paciente> findConAlergiasById(Long id);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    @Query(value = "select nextval('historia_clinica_seq')", nativeQuery = true)
    long siguienteHistoria();

    @Query("""
            select p from Paciente p
            where (:estado is null or p.estado = :estado)
              and (:q = ''
                   or p.dni like concat(:q, '%')
                   or upper(p.historiaClinica) = upper(:q)
                   or lower(concat(p.apellidos, ' ', p.nombres)) like concat('%', lower(:q), '%')
                   or lower(concat(p.nombres, ' ', p.apellidos)) like concat('%', lower(:q), '%'))
            """)
    Page<Paciente> buscar(@Param("q") String q, @Param("estado") EstadoPaciente estado, Pageable pageable);
}
