package edu.senacsp.astrochimp;

import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.AlunoRepository;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import edu.senacsp.astrochimp.service.AcessoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private ProfessorRepository professores;
    private AcessoService acesso;

    @BeforeEach
    void preparar() {
        // Os repositórios simulados devolvem apenas os dados definidos em cada teste.
        alunos = mock(AlunoRepository.class);
        salas = mock(SalaRepository.class);
        professores = mock(ProfessorRepository.class);
        acesso = new AcessoService(professores, salas, alunos);
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

    @Test
    void salaJaConsultadaNaoPrecisaSerBuscadaNovamenteParaConferirAluno() {
        Aluno aluno = new Aluno();
        aluno.setId(10L);
        aluno.setToken("token-do-aluno");
        Sala sala = new Sala();
        sala.getAlunos().add(aluno);
        when(alunos.findByToken("token-do-aluno")).thenReturn(Optional.of(aluno));

        assertSame(aluno, acesso.alunoDaSala(sala, "token-do-aluno"));
        // O token ainda é consultado, mas a sala já recebida não exige outra busca no banco.
        verify(alunos).findByToken("token-do-aluno");
        verifyNoInteractions(salas, professores);
    }

    @Test
    void donoJaCarregadoNaSalaNaoPrecisaSerBuscadoPeloId() {
        Professor professor = new Professor();
        professor.setId(1L);
        professor.setEmail("ana@exemplo.com");
        Sala sala = new Sala();
        sala.setProfessor(professor);
        when(salas.findById(2L)).thenReturn(Optional.of(sala));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@exemplo.com", "senha", new ArrayList<>()));
        try {
            assertSame(sala, acesso.salaDoProfessor(2L));
            // A comparação usa o e-mail do dono da sala; não repete a busca do professor pelo id.
            verifyNoInteractions(professores);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
