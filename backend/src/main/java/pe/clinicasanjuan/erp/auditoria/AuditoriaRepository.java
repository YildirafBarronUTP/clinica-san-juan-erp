package pe.clinicasanjuan.erp.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    @Query("""
            select a from RegistroAuditoria a
            where (:entidad is null or a.entidad = :entidad)
              and a.fecha >= :desde and a.fecha < :hasta
            order by a.fecha desc, a.id desc
            """)
    Page<RegistroAuditoria> buscar(@Param("entidad") String entidad, @Param("desde") Instant desde,
                                   @Param("hasta") Instant hasta, Pageable pageable);
}
