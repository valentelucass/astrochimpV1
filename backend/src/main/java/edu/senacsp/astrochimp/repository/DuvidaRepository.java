package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Duvida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DuvidaRepository extends JpaRepository<Duvida, Long> {
    List<Duvida> findBySalaId(Long salaId);
}
