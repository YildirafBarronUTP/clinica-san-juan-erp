package pe.clinicasanjuan.erp.farmacia.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.clinicasanjuan.erp.farmacia.model.Producto;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Carga el producto incrementando su versión al confirmar: dos movimientos
     * simultáneos del mismo producto no pueden confirmarse ambos (bloqueo optimista).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findParaMovimiento(@Param("id") Long id);

    @Query("""
            select p from Producto p
            where (:q = '' or lower(p.nombre) like concat('%', lower(:q), '%')
                   or lower(p.principioActivo) like concat('%', lower(:q), '%')
                   or upper(p.codigo) = upper(:q))
              and (:soloActivos = false or p.activo = true)
            """)
    Page<Producto> buscar(@Param("q") String q, @Param("soloActivos") boolean soloActivos, Pageable pageable);
}
