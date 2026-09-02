package com.personalProject.CriptoAnaliser.controller;

import com.personalProject.CriptoAnaliser.model.dto.PrecoAtualDTO;
import com.personalProject.CriptoAnaliser.service.PrecoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/precos")
@CrossOrigin(origins = "*")
public class PrecoController {
    private final PrecoService service;

    public PrecoController(PrecoService service) {
        this.service = service;
    }

    @GetMapping("/atuais")
    public ResponseEntity<List<PrecoAtualDTO>> atuais(
            @RequestParam(required = false) String simbolo) {

        List<PrecoAtualDTO> precos = service.atuais(simbolo);
        return precos.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(precos);

    }
}
