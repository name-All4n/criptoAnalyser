package com.personalProject.CriptoAnaliser.service;

import com.personalProject.CriptoAnaliser.model.Preco;
import com.personalProject.CriptoAnaliser.model.dto.PrecoAtualDTO;
import com.personalProject.CriptoAnaliser.repository.PrecoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PrecoService {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final PrecoRepository repository;

    public PrecoService(PrecoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PrecoAtualDTO> atuais(String simbolo) {
        OffsetDateTime agora = OffsetDateTime.now();

        return repository.buscarUltimaColeta().stream()
                .filter(p -> simbolo == null || p.getSimbolo().equalsIgnoreCase(simbolo))
                .map(p -> converter(p, agora))
                .toList();
    }

    private PrecoAtualDTO converter(Preco p, OffsetDateTime agora) {
        return new PrecoAtualDTO(
                p.getColetaId(),
                p.getColetadoEm(),
                p.getSimbolo(),
                p.getExchange(),
                p.getMelhorCompra(),
                p.getMelhorVenda(),
                p.getTaxaTaker(),
                spreadInterno(p),
                Duration.between(p.getColetadoEm(), agora).toSeconds()
        );
    }

    private BigDecimal spreadInterno(Preco p) {
        BigDecimal compra = p.getMelhorCompra();
        if (compra == null || compra.signum() == 0) {
            return null;
        }
        return p.getMelhorVenda()
                .subtract(compra)
                .divide(compra, 10, RoundingMode.HALF_UP)
                .multiply(CEM)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
