package edu.senacsp.astrochimp;

import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.AlunoRepository;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import edu.senacsp.astrochimp.service.AcessoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Testa as regras de acesso isoladamente, sem iniciar a API nem alterar o banco.
class AcessoServiceTest {
    private AlunoRepository alunos;
    private SalaRepository salas;
    private AcessoService acesso;

    @BeforeEach
    void preparar() {
        // Os repositórios simulados devolvem apenas os dados definidos em cada teste.
        alunos = mock(AlunoRepository.class);
        salas = mock(SalaRepository.class);
        acesso = new AcessoService(mock(ProfessorRepository.class), salas, alunos);
    }

    @Test
    void salaAtivaComPrazoFuturoPermiteInteracao() {
        Sala sala = new Sala();
        sala.setStatus("ativa");
        sala.setExpiraEm(LocalDateTime.now().plusHours(1));
        assertDoesNotThrow(() -> acesso.exigirSalaAtiva(sala));
    }

    @Test
    void salaEncerradaOuExpiradaRecusaInteracao() {
        // Uma lista reúne as duas situações que precisam bloquear a sala.
        ArrayList<Sala> salasBloqueadas = new ArrayList<>();
        Sala encerrada = new Sala();
        encerrada.setStatus("encerrada");
        salasBloqueadas.add(encerrada);
        Sala expirada = new Sala();
        expirada.setStatus("ativa");
        expirada.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        salasBloqueadas.add(expirada);
        for (Sala sala : salasBloqueadas) {
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> acesso.exigirSalaAtiva(sala));
            assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        }
    }

    @Test
    void tokenAusenteOuInvalidoNaoAutorizaAluno() {
        when(alunos.findByToken("errado")).thenReturn(Optional.empty());
        for (String token : new String[]{"", "errado"}) {
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> acesso.alunoDaSala(1L, token));
            assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        }
    }

    @Test
    void tokenComLetrasDiferentesNaoPodeSerAceito() {
        Aluno aluno = new Aluno();
        aluno.setToken("Abc123");
        // Mesmo que a consulta encontre o aluno, o token deve ser exatamente igual.
        when(alunos.findByToken("abc123")).thenReturn(Optional.of(aluno));
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> acesso.alunoDaSala(1L, "abc123"));
        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    void apenasParticipanteDaSalaPodeUsarToken() {
        Aluno aluno = new Aluno();
        aluno.setId(10L);
        aluno.setToken("token-do-aluno");
        Sala sala = new Sala();
        when(alunos.findByToken("token-do-aluno")).thenReturn(Optional.of(aluno));
        when(salas.findById(1L)).thenReturn(Optional.of(sala));
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> acesso.alunoDaSala(1L, "token-do-aluno"));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        sala.getAlunos().add(aluno);
        assertSame(aluno, acesso.alunoDaSala(1L, "token-do-aluno"));
    }
}
