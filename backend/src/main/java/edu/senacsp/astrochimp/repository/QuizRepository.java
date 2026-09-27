package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByProfessorIdAndSalvoNoPerfilTrue(Long professorId);
}