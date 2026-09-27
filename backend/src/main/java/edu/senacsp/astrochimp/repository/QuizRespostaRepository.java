package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.QuizResposta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRespostaRepository extends JpaRepository<QuizResposta, Long> {

    boolean existsBySessaoIdAndAlunoIdAndAlternativa_Pergunta_Id(String sessaoId, Long alunoId, Long perguntaId);
    long countBySessaoIdAndAlternativaId(String sessaoId, Long alternativaId);
    boolean existsByAlternativa_Pergunta_Quiz_Id(Long quizId);
}
