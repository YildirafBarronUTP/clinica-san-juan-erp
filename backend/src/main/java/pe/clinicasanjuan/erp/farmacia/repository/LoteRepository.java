package pe.clinicasanjuan.erp.farmacia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.clinicasanjuan.erp.farmacia.model.Lote;

import java.time.LocalDate;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    boolean existsByProductoIdAndNumeroLoteIgnoreCase(Long productoId, String numeroLote);

    /** Lotes con saldo y no vencidos, en orden FEFO (primero el que vence primero). */
    @Query("""
            select l from Lote l
            where l.producto.id = :productoId and l.cantidadActual > 0 and l.fechaVencimiento >= :hoy
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<Lote> disponiblesFefo(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    List<Lote> findByProductoIdOrderByFechaVencimientoAscIdAsc(Long productoId);

    @Query("select coalesce(sum(l.cantidadActual), 0) from Lote l where l.producto.id = :productoId")
    int stockFisico(@Param("productoId") Long productoId);

    @Query("""
            select coalesce(sum(l.cantidadActual), 0) from Lote l
            where l.producto.id = :productoId and l.fechaVencimiento >= :hoy
            """)
    int stockDisponible(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    @Query("""
            select l from Lote l join fetch l.producto
            where l.cantidadActual > 0 and l.fechaVencimiento <= :limite
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<Lote> conSaldoQueVencenHasta(@Param("limite") LocalDate limite);

    /** [productoId, stockFisico, stockDisponible] de todos los productos activos. */
    @Query("""
            select p.id,
                   coalesce(sum(l.cantidadActual), 0),
                   coalesce(sum(case when l.fechaVencimiento >= :hoy then l.cantidadActual else 0 end), 0)
            from Producto p left join Lote l on l.producto = p
            where p.activo = true
            group by p.id
            """)
    List<Object[]> stocksPorProducto(@Param("hoy") LocalDate hoy);
}
