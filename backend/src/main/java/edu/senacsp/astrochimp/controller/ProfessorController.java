package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.CadastrarProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilPublicoProfessorDto;
import edu.senacsp.astrochimp.service.ProfessorService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilProfessorDto cadastrar(@RequestBody CadastrarProfessorDto pedido) {
        return professorService.cadastrar(pedido);
    }

    @GetMapping("/{id}")
    public PerfilProfessorDto buscar(@PathVariable("id") Long professorId) {
        return professorService.buscar(professorId);
    }

    @PutMapping("/{id}")
    public PerfilProfessorDto atualizar(
            @PathVariable("id") Long professorId,
            @RequestBody PerfilProfessorDto pedido) {
        return professorService.atualizar(professorId, pedido);
    }

    @GetMapping("/publico/{slug}")
    public PerfilPublicoProfessorDto publico(@PathVariable String slug) {
        return professorService.publico(slug);
    }
}
