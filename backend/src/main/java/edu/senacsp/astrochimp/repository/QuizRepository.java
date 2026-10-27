package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Quiz pelo id Long.
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // O nome faz o Spring buscar quizzes desse professor com salvoNoPerfil = true.
    List<Quiz> findByProfessorIdAndSalvoNoPerfilTrue(Long professorId);
}