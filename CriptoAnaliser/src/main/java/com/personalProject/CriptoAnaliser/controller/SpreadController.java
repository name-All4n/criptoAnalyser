package com.personalProject.CriptoAnaliser.controller;

import com.personalProject.CriptoAnaliser.model.dto.HistoricoDTO;
import com.personalProject.CriptoAnaliser.model.dto.SpreadAtualDTO;
import com.personalProject.CriptoAnaliser.service.HistoricoService;
import com.personalProject.CriptoAnaliser.service.SpreadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spreads")
@CrossOrigin(origins = "*")
public class SpreadController {
    private final SpreadService spreadService;
    private final HistoricoService historicoService;

    public SpreadController(SpreadService spreadService, HistoricoService historicoService) {
        this.spreadService = spreadService;
        this.historicoService = historicoService;
    }
    @GetMapping("/atuais")
    public ResponseEntity<List<SpreadAtualDTO>> atuais(
            @RequestParam(required = false) String simbolo,
            @RequestParam(defaultValue = "false") boolean apenasMelhor,
            @RequestParam(defaultValue = "20") int limite) {

        int limiteSeguro = Math.min(Math.max(limite, 1), 200);
        List<SpreadAtualDTO> spreads = spreadService.atuais(simbolo, apenasMelhor, limiteSeguro);

        return spreads.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(spreads);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<HistoricoDTO>> historico(
            @RequestParam(required = false) String simbolo) {

        List<HistoricoDTO> hist = historicoService.ultimos7Dias(simbolo);
        return hist.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(hist);
    }
}
