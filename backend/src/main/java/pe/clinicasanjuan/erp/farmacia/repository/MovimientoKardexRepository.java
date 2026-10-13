package pe.clinicasanjuan.erp.farmacia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.clinicasanjuan.erp.farmacia.model.MovimientoKardex;

import java.util.List;

public interface MovimientoKardexRepository extends JpaRepository<MovimientoKardex, Long> {
    @Query("""
            select m from MovimientoKardex m left join fetch m.lote
            where m.producto.id = :productoId
            order by m.fecha asc, m.id asc
            """)
    List<MovimientoKardex> deProducto(@Param("productoId") Long productoId);
}
