package com.personalProject.CriptoAnaliser.service;

import com.personalProject.CriptoAnaliser.model.dto.HistoricoDTO;
import com.personalProject.CriptoAnaliser.repository.SpreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class HistoricoService {
    private final SpreadRepository repository;

    public HistoricoService(SpreadRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HistoricoDTO> ultimos7Dias(String simbolo) {
        List<Object[]> raw = repository.buscarHistorico7Dias();
        List<HistoricoDTO> result = new ArrayList<>();

        for (Object[] row : raw) {
            String sym = (String) row[1];
            if (simbolo != null && !sym.equalsIgnoreCase(simbolo)) continue;

            OffsetDateTime dt = toOffsetDateTime(row[0]);
            BigDecimal lucro = (BigDecimal) row[2];
            String rota = row[3] + " → " + row[4];

            result.add(new HistoricoDTO(dt, sym, lucro, rota, 1));
        }

        return result;
    }

    private static OffsetDateTime toOffsetDateTime(Object valor) {
        if (valor instanceof OffsetDateTime o) return o;
        if (valor instanceof Instant i) return i.atOffset(ZoneOffset.UTC);
        if (valor instanceof java.sql.Timestamp t) return t.toInstant().atOffset(ZoneOffset.UTC);
        if (valor instanceof java.time.LocalDateTime l) return l.atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("tipo inesperado em coletado_em: " + valor.getClass());
    }
}
