package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.QuizResposta;
import org.springframework.data.jpa.repository.JpaRepository;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir QuizResposta pelo id Long.
public interface QuizRespostaRepository extends JpaRepository<QuizResposta, Long> {

    // O Spring monta a consulta pelo nome: verifica se o aluno já respondeu a pergunta nessa rodada (sessaoId).
    boolean existsBySessaoIdAndAlunoIdAndAlternativa_Pergunta_Id(String sessaoId, Long alunoId, Long perguntaId);
    // Conta quantas respostas marcaram esta alternativa nessa rodada do quiz.
    long countBySessaoIdAndAlternativaId(String sessaoId, Long alternativaId);
    // Segue as relações resposta -> alternativa -> pergunta -> quiz para verificar se o quiz tem alguma resposta.
    boolean existsByAlternativa_Pergunta_Quiz_Id(Long quizId);
}
