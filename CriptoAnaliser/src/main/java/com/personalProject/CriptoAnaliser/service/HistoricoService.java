package com.personalProject.CriptoAnaliser.service;

import com.personalProject.CriptoAnaliser.model.dto.HistoricoDTO;
import com.personalProject.CriptoAnaliser.repository.SpreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
            Timestamp ts = (Timestamp) row[0];
            String sym = (String) row[1];
            BigDecimal lucro = (BigDecimal) row[2];
            String exCompra = (String) row[3];
            String exVenda = (String) row[4];

            if (simbolo != null && !sym.equalsIgnoreCase(simbolo)) continue;

            OffsetDateTime dt = ts.toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime();
            String rota = exCompra + " → " + exVenda;

            result.add(new HistoricoDTO(dt, sym, lucro, rota, 1));
        }

        return result;
    }
}
