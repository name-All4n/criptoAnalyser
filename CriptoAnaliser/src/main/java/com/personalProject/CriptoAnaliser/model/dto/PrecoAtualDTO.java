package com.personalProject.CriptoAnaliser.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PrecoAtualDTO(
        UUID coletaId,
        OffsetDateTime coletadoEm,
        String simbolo,
        String exchange,
        BigDecimal melhorCompra,
        BigDecimal melhorVenda,
        BigDecimal taxaTaker,
        BigDecimal spreadInternoPct,
        long idadeSegundos
){
}
