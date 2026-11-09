package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.EntradaSalaDto;
import edu.senacsp.astrochimp.dto.SalaDto;
import edu.senacsp.astrochimp.service.SalaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// O Spring registra este controller para atender as rotas de salas e devolver os dados em JSON.
@RestController
// Faz as rotas desta classe começarem com /api/salas.
@RequestMapping("/api/salas")
public class SalaController {
    private final SalaService salaService;

    // O Spring passa o SalaService criado por ele para este controller usar nas rotas.
    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    // @PostMapping liga o POST /api/salas à criação da sala; o prefixo vem de @RequestMapping.
    @PostMapping
    // Devolve 201 para avisar que a sala foi criada.
    @ResponseStatus(HttpStatus.CREATED)
    // @RequestBody monta SalaDto com professorId e nomeAula recebidos no JSON.
    public SalaDto criar(@RequestBody SalaDto pedido) {
        return salaService.criar(pedido);
    }

    // @GetMapping liga o GET /api/salas/{id} à consulta dos dados da sala.
    @GetMapping("/{id}")
    public SalaDto buscar(
            // Pega o id no endereço e coloca em salaId.
            @PathVariable("id") Long salaId,
            // @RequestHeader lê X-Aluno-Token; sem esse cabeçalho, o service exige o login do professor dono da sala.
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return salaService.buscar(salaId, tokenAluno);
    }

    @PostMapping("/entrar")
    // O pedido traz codigo e nomeAluno; a resposta de entrada devolve os dados de acesso à sala.
    public EntradaSalaDto entrar(@RequestBody EntradaSalaDto pedido) {
        return salaService.entrar(pedido);
    }

    // @PatchMapping liga o PATCH /api/salas/{id}/encerrar ao encerramento da sala.
    @PatchMapping("/{id}/encerrar")
    public SalaDto encerrar(@PathVariable("id") Long salaId) {
        return salaService.encerrar(salaId);
    }

    @PostMapping("/{id}/cronometro")
    public int iniciarCronometro(
            @PathVariable("id") Long salaId,
            // O corpo JSON traz apenas um número, como 120, para definir a duração em segundos.
            @RequestBody Integer segundos) {
        return salaService.iniciarCronometro(salaId, segundos);
    }

    @GetMapping("/{id}/cronometro")
    // Devolve os segundos restantes; zero indica que não há uma contagem ativa.
    public int consultarCronometro(
            @PathVariable("id") Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return salaService.consultarCronometro(salaId, tokenAluno);
    }

    // @DeleteMapping liga esta rota DELETE ao pedido para parar o cronômetro da sala.
    @DeleteMapping("/{id}/cronometro")
    public int pararCronometro(@PathVariable("id") Long salaId) {
        return salaService.pararCronometro(salaId);
    }
}
