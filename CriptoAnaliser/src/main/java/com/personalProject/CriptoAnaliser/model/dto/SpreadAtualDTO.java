package com.personalProject.CriptoAnaliser.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpreadAtualDTO(
        UUID coletaId,
        OffsetDateTime coletadoEm,
        String simbolo,
        String rota,
        String exchangeCompra,
        String exchangeVenda,
        BigDecimal volumeUsdt,
        BigDecimal lucroPct,
        BigDecimal lucroUsdt,
        String status,
        long idadeSegundos,
        boolean dadoVelho
) {
}
