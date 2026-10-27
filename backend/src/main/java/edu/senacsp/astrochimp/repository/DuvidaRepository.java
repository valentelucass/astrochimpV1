package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Duvida;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Duvida pelo id Long.
public interface DuvidaRepository extends JpaRepository<Duvida, Long> {
    // Primeiro as fixadas, depois as mais votadas e, em caso de empate, as mais antigas.
    // @Query define a consulta de Duvida desta sala: :salaId recebe o argumento salaId do método.
    @Query("SELECT d FROM Duvida d WHERE d.sala.id = :salaId ORDER BY d.fixada DESC, SIZE(d.alunosQueVotaram) DESC, d.criadaEm ASC")
    List<Duvida> listarPorSala(Long salaId);

    // @Lock reserva o registro desta dúvida no banco até terminar a transação; outras requisições que precisam do bloqueio aguardam.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Duvida d WHERE d.id = :duvidaId")
    Optional<Duvida> buscarParaAtualizar(Long duvidaId);
}
