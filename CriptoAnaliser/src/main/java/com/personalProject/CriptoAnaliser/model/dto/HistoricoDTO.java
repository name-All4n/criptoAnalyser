package com.personalProject.CriptoAnaliser.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HistoricoDTO(
        OffsetDateTime momento,
        String simbolo,
        BigDecimal lucroPct,
        String melhorRota,
        int amostras
) {
}
