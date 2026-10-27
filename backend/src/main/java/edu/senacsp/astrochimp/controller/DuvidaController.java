package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.DuvidaDto;
import edu.senacsp.astrochimp.service.DuvidaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// O Spring registra este controller para atender as rotas de dúvidas e devolver os DTOs em JSON.
@RestController
public class DuvidaController {
    private final DuvidaService duvidaService;

    // O Spring passa o DuvidaService criado por ele para este controller usar nas rotas.
    public DuvidaController(DuvidaService duvidaService) {
        this.duvidaService = duvidaService;
    }

    // @PostMapping liga o POST /api/salas/{salaId}/duvidas ao método que pede a criação da dúvida.
    @PostMapping("/api/salas/{salaId}/duvidas")
    // Devolve 201 para avisar que a dúvida foi criada.
    @ResponseStatus(HttpStatus.CREATED)
    public DuvidaDto criar(
            // Pega salaId no endereço para saber em qual sala criar a dúvida.
            @PathVariable Long salaId,
            // Lê X-Aluno-Token no cabeçalho para identificar o aluno.
            @RequestHeader("X-Aluno-Token") String tokenAluno,
            // Monta DuvidaDto com o texto e a opção de anonimato recebidos no JSON.
            @RequestBody DuvidaDto pedido) {
        return duvidaService.criar(salaId, tokenAluno, pedido);
    }

    // @GetMapping liga o GET /api/salas/{salaId}/duvidas à consulta das dúvidas para os alunos.
    @GetMapping("/api/salas/{salaId}/duvidas")
    public List<DuvidaDto> listarAluno(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String tokenAluno) {
        return duvidaService.listar(salaId, tokenAluno, false);
    }

    @GetMapping("/api/salas/{salaId}/duvidas/professor")
    public List<DuvidaDto> listarProfessor(@PathVariable Long salaId) {
        return duvidaService.listar(salaId, null, true);
    }

    @PostMapping("/api/duvidas/{id}/votos")
    public DuvidaDto votar(@PathVariable("id") Long duvidaId, @RequestHeader("X-Aluno-Token") String tokenAluno) {
        return duvidaService.votar(duvidaId, tokenAluno);
    }

    // @PatchMapping liga o PATCH /api/duvidas/{id}/estado à alteração do estado da dúvida.
    @PatchMapping("/api/duvidas/{id}/estado")
    public DuvidaDto mudarEstado(@PathVariable("id") Long duvidaId, @RequestBody DuvidaDto pedido) {
        return duvidaService.mudarEstado(duvidaId, pedido);
    }

    @PatchMapping("/api/duvidas/{id}/fixacao")
    public DuvidaDto fixar(@PathVariable("id") Long duvidaId, @RequestBody DuvidaDto pedido) {
        return duvidaService.fixar(duvidaId, pedido);
    }
}
