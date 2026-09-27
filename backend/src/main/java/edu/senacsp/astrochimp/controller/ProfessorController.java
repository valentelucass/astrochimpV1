package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.AtualizarProfessorDto;
import edu.senacsp.astrochimp.dto.CadastrarProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilPublicoProfessorDto;
import edu.senacsp.astrochimp.service.ProfessorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorService professores;

    public ProfessorController(ProfessorService professores) {
        this.professores = professores;
    }

    @PostMapping
    public ResponseEntity<PerfilProfessorDto> cadastrar(@RequestBody CadastrarProfessorDto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(professores.cadastrar(pedido));
    }

    @GetMapping("/{id}")
    public PerfilProfessorDto buscar(@PathVariable Long id) {
        return professores.buscar(id);
    }

    @PutMapping("/{id}")
    public PerfilProfessorDto atualizar(
            @PathVariable Long id,
            @RequestBody AtualizarProfessorDto pedido) {
        return professores.atualizar(id, pedido);
    }

    @GetMapping("/publico/{slug}")
    public PerfilPublicoProfessorDto publico(@PathVariable String slug) {
        return professores.publico(slug);
    }
}
