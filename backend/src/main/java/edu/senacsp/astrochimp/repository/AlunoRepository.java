package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Aluno pelo id Long.
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    // O nome faz o Spring consultar o campo token do aluno; Optional pode vir vazio se o token não existir.
    Optional<Aluno> findByToken(String tokenAluno);
}
