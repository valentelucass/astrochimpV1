package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByProfessorId(Long professorId);
    List<Material> findByProfessorIdAndPublicoTrue(Long professorId);
}
