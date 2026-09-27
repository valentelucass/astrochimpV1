package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    Optional<Sala> findByCodigo(String codigo);
    boolean existsByQuizEmAndamentoId(Long quizId);
}
