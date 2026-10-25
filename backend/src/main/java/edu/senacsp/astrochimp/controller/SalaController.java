package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.CriarSalaDto;
import edu.senacsp.astrochimp.dto.CronometroDto;
import edu.senacsp.astrochimp.dto.EntradaSalaDto;
import edu.senacsp.astrochimp.dto.EntrarSalaDto;
import edu.senacsp.astrochimp.dto.IniciarCronometroDto;
import edu.senacsp.astrochimp.dto.SalaDto;
import edu.senacsp.astrochimp.service.SalaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salas")
public class SalaController {
    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaDto criar(@RequestBody CriarSalaDto pedido) {
        return salaService.criar(pedido);
    }

    @GetMapping("/{id}")
    public SalaDto buscar(
            @PathVariable("id") Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return salaService.buscar(salaId, tokenAluno);
    }

    @PostMapping("/entrar")
    public EntradaSalaDto entrar(@RequestBody EntrarSalaDto pedido) {
        return salaService.entrar(pedido);
    }

    @PatchMapping("/{id}/encerrar")
    public SalaDto encerrar(@PathVariable("id") Long salaId) {
        return salaService.encerrar(salaId);
    }

    @PostMapping("/{id}/cronometro")
    public CronometroDto iniciarCronometro(
            @PathVariable("id") Long salaId,
            @RequestBody IniciarCronometroDto pedido) {
        return salaService.iniciarCronometro(salaId, pedido);
    }

    @GetMapping("/{id}/cronometro")
    public CronometroDto consultarCronometro(
            @PathVariable("id") Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return salaService.consultarCronometro(salaId, tokenAluno);
    }

    @DeleteMapping("/{id}/cronometro")
    public CronometroDto pararCronometro(@PathVariable("id") Long salaId) {
        return salaService.pararCronometro(salaId);
    }
}
