package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.DuvidaDto;
import edu.senacsp.astrochimp.service.DuvidaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DuvidaController {
    private final DuvidaService duvidaService;

    public DuvidaController(DuvidaService duvidaService) {
        this.duvidaService = duvidaService;
    }

    @PostMapping("/api/salas/{salaId}/duvidas")
    @ResponseStatus(HttpStatus.CREATED)
    public DuvidaDto criar(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String tokenAluno,
            @RequestBody DuvidaDto pedido) {
        return duvidaService.criar(salaId, tokenAluno, pedido);
    }

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

    @PatchMapping("/api/duvidas/{id}/estado")
    public DuvidaDto mudarEstado(@PathVariable("id") Long duvidaId, @RequestBody DuvidaDto pedido) {
        return duvidaService.mudarEstado(duvidaId, pedido);
    }

    @PatchMapping("/api/duvidas/{id}/fixacao")
    public DuvidaDto fixar(@PathVariable("id") Long duvidaId, @RequestBody DuvidaDto pedido) {
        return duvidaService.fixar(duvidaId, pedido);
    }
}
