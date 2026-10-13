package pe.clinicasanjuan.erp.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.clinicasanjuan.erp.seguridad.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
