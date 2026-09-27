package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.AlterarEstadoDuvidaDto;
import edu.senacsp.astrochimp.dto.CriarDuvidaDto;
import edu.senacsp.astrochimp.dto.DuvidaDto;
import edu.senacsp.astrochimp.dto.FixarDuvidaDto;
import edu.senacsp.astrochimp.service.DuvidaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DuvidaController {
    private final DuvidaService duvidas;

    public DuvidaController(DuvidaService duvidas) {
        this.duvidas = duvidas;
    }

    @PostMapping("/api/salas/{salaId}/duvidas")
    public ResponseEntity<DuvidaDto> criar(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String token,
            @RequestBody CriarDuvidaDto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(duvidas.criar(salaId, token, pedido));
    }

    @GetMapping("/api/salas/{salaId}/duvidas")
    public List<DuvidaDto> listarAluno(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String token) {
        return duvidas.listar(salaId, token, false);
    }

    @GetMapping("/api/salas/{salaId}/duvidas/professor")
    public List<DuvidaDto> listarProfessor(@PathVariable Long salaId) {
        return duvidas.listar(salaId, null, true);
    }

    @PostMapping("/api/duvidas/{id}/votos")
    public DuvidaDto votar(@PathVariable Long id, @RequestHeader("X-Aluno-Token") String token) {
        return duvidas.votar(id, token);
    }

    @PatchMapping("/api/duvidas/{id}/estado")
    public DuvidaDto mudarEstado(@PathVariable Long id, @RequestBody AlterarEstadoDuvidaDto pedido) {
        return duvidas.mudarEstado(id, pedido);
    }

    @PatchMapping("/api/duvidas/{id}/fixacao")
    public DuvidaDto fixar(@PathVariable Long id, @RequestBody FixarDuvidaDto pedido) {
        return duvidas.fixar(id, pedido);
    }
}
