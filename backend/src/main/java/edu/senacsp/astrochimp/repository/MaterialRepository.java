package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Material pelo id Long.
public interface MaterialRepository extends JpaRepository<Material, Long> {
    // O Spring busca materiais pelo id do professor associado, conforme o nome do método.
    List<Material> findByProfessorId(Long professorId);
    // Busca os materiais desse professor com publico = true.
    List<Material> findByProfessorIdAndPublicoTrue(Long professorId);
}
