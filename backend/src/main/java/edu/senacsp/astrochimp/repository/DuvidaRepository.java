package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Duvida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DuvidaRepository extends JpaRepository<Duvida, Long> {
    // Primeiro as fixadas, depois as mais votadas e, em caso de empate, as mais antigas.
    @Query("SELECT d FROM Duvida d WHERE d.sala.id = :salaId ORDER BY d.fixada DESC, SIZE(d.alunosQueVotaram) DESC, d.criadaEm ASC")
    List<Duvida> listarPorSala(Long salaId);
}
