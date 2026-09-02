package com.personalProject.CriptoAnaliser.service;

import com.personalProject.CriptoAnaliser.model.Spread;
import com.personalProject.CriptoAnaliser.model.dto.SpreadAtualDTO;
import com.personalProject.CriptoAnaliser.repository.SpreadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SpreadService {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final SpreadRepository repository;

    /** Acima disso a rota é candidata a alerta. Configurável. */
    @Value("${cripto.alerta.lucro-minimo-pct:0.30}")
    private BigDecimal lucroMinimoAlerta;

    /** Passou disso, o dado não serve pra decisão. */
    @Value("${cripto.dados.idade-maxima-segundos:120}")
    private long idadeMaximaSegundos;

    public SpreadService(SpreadRepository repository) {
        this.repository = repository;
    }

    /**
     * @param simbolo   filtro opcional, ex. "BTC/USDT"
     * @param apenasMelhor true = só a melhor rota de cada símbolo
     * @param limite    máximo de linhas devolvidas
     */
    @Transactional(readOnly = true)
    public List<SpreadAtualDTO> atuais(String simbolo, boolean apenasMelhor, int limite) {
        OffsetDateTime agora = OffsetDateTime.now();

        List<Spread> encontrados = apenasMelhor
                ? repository.buscarMelhorPorSimbolo()
                : repository.buscarUltimaColeta();

        return encontrados.stream()
                .filter(s -> simbolo == null || s.getSimbolo().equalsIgnoreCase(simbolo))
                .sorted(Comparator.comparing(Spread::getLucroPct).reversed())
                .limit(limite)
                .map(s -> converter(s, agora))
                .toList();
    }

    private SpreadAtualDTO converter(Spread s, OffsetDateTime agora) {
        long idade = Duration.between(s.getColetadoEm(), agora).toSeconds();

        return new SpreadAtualDTO(
                s.getColetaId(),
                s.getColetadoEm(),
                s.getSimbolo(),
                s.getExchangeCompra() + " -> " + s.getExchangeVenda(),
                s.getExchangeCompra(),
                s.getExchangeVenda(),
                s.getVolumeUsdt(),
                s.getLucroPct().setScale(4, RoundingMode.HALF_UP),
                lucroEmUsdt(s),
                classificar(s.getLucroPct()),
                idade,
                idade > idadeMaximaSegundos
        );
    }

    /** lucro_pct é percentual, então divide por 100 antes de aplicar ao volume. */
    private BigDecimal lucroEmUsdt(Spread s) {
        return s.getVolumeUsdt()
                .multiply(s.getLucroPct())
                .divide(CEM, 2, RoundingMode.HALF_UP);
    }

    private String classificar(BigDecimal lucroPct) {
        if (lucroPct.compareTo(lucroMinimoAlerta) >= 0) return "OPORTUNIDADE";
        if (lucroPct.signum() > 0)                     return "OBSERVACAO";
        return "NEGATIVA";
    }
}
