package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findByToken(String token);
}
