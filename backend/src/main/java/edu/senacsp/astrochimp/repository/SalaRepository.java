package edu.senacsp.astrochimp.repository;

import edu.senacsp.astrochimp.entity.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

// O Spring Data cria este repositório; JpaRepository oferece salvar, buscar e excluir Sala pelo id Long.
public interface SalaRepository extends JpaRepository<Sala, Long> {

    // O nome faz o Spring consultar o código de entrada da sala; Optional pode vir vazio se o código não existir.
    Optional<Sala> findByCodigo(String codigo);
    // O painel recebe as salas do professor informado, ordenadas da criação mais nova para a mais antiga.
    List<Sala> findByProfessorIdOrderByCriadaEmDesc(Long professorId);
    // Verifica se alguma sala está usando este quiz no campo quizEmAndamento.
    boolean existsByQuizEmAndamentoId(Long quizId);

    // @Lock reserva o registro desta sala no banco até terminar a transação; outras requisições que precisam do bloqueio aguardam.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query busca a sala pelo id recebido em salaId para registrar a resposta do aluno com o bloqueio.
    @Query("SELECT s FROM Sala s WHERE s.id = :salaId")
    Optional<Sala> buscarParaAtualizar(Long salaId);
}
