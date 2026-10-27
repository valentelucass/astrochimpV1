package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Professor pelo id Long.
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    // O nome faz o Spring consultar o campo email; Optional pode vir vazio se não houver esse professor.
    Optional<Professor> findByEmail(String email);
    // Busca o professor pelo texto que identifica seu perfil no endereço público.
    Optional<Professor> findBySlugPerfilPublico(String slugPerfilPublico);
}
