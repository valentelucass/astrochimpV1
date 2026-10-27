package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.QuizAlternativa;
import org.springframework.data.jpa.repository.JpaRepository;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir QuizAlternativa pelo id Long.
public interface QuizAlternativaRepository extends JpaRepository<QuizAlternativa, Long> {
}