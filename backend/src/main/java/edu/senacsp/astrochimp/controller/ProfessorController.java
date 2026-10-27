package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.PerfilProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilPublicoProfessorDto;
import edu.senacsp.astrochimp.dto.SalaDto;
import edu.senacsp.astrochimp.service.ProfessorService;
import edu.senacsp.astrochimp.service.SalaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// O Spring registra este controller para atender as rotas de professores e devolver os perfis em JSON.
@RestController
// Faz as rotas desta classe começarem com /api/professores.
@RequestMapping("/api/professores")
public class ProfessorController {
    private final ProfessorService professorService;
    private final SalaService salaService;

    // O Spring passa o ProfessorService criado por ele para este controller usar nas rotas.
    public ProfessorController(ProfessorService professorService, SalaService salaService) {
        this.professorService = professorService;
        this.salaService = salaService;
    }

    // @PostMapping liga o POST /api/professores ao cadastro; o prefixo vem de @RequestMapping.
    @PostMapping
    // Devolve 201 para avisar que o professor foi cadastrado.
    @ResponseStatus(HttpStatus.CREATED)
    // @RequestBody: Monta PerfilProfessorDto com nome, e-mail e senha recebidos no JSON.
    public PerfilProfessorDto cadastrar(@RequestBody PerfilProfessorDto pedido) {
        return professorService.cadastrar(pedido);
    }

    // @GetMapping liga o GET /api/professores/{id} à consulta do perfil do professor.
    @GetMapping("/{id}")
    // @PathVariable: Pega o id no endereço e coloca em professorId.
    public PerfilProfessorDto buscar(@PathVariable("id") Long professorId) {
        return professorService.buscar(professorId);
    }

    @GetMapping("/me")
    public PerfilProfessorDto conectado(Authentication login) {
        // O Spring já conferiu o login Basic; o app não precisa pedir o id do professor na tela de entrada.
        return professorService.buscarPorEmail(login.getName());
    }

    @GetMapping("/{id}/salas")
    public List<SalaDto> salas(@PathVariable("id") Long professorId) {
        return salaService.listarDoProfessor(professorId);
    }

    // @PutMapping liga o PUT /api/professores/{id} à atualização do perfil do professor.
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
