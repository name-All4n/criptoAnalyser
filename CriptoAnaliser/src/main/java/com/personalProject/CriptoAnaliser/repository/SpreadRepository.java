package com.personalProject.CriptoAnaliser.repository;

import com.personalProject.CriptoAnaliser.model.Spread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpreadRepository extends JpaRepository<Spread, Long> {

    /** Todas as rotas da última coleta gravada, melhores primeiro. */
    @Query(value = """
            SELECT * FROM spreads
            WHERE coleta_id = (
                SELECT coleta_id FROM spreads
                WHERE coleta_id IS NOT NULL
                ORDER BY coletado_em DESC
                LIMIT 1
            )
            ORDER BY lucro_pct DESC
            """, nativeQuery = true)
    List<Spread> buscarUltimaColeta();

    /** Melhor rota por símbolo na última coleta. Um card por par no painel. */
    @Query(value = """
            SELECT DISTINCT ON (simbolo) *
            FROM spreads
            WHERE coleta_id = (
                SELECT coleta_id FROM spreads
                WHERE coleta_id IS NOT NULL
                ORDER BY coletado_em DESC
                LIMIT 1
            )
            ORDER BY simbolo, lucro_pct DESC
            """, nativeQuery = true)
    List<Spread> buscarMelhorPorSimbolo();

    @Query(value = """
            SELECT s.coletado_em, s.simbolo, s.lucro_pct, s.exchange_compra, s.exchange_venda
            FROM spreads s
            WHERE s.coletado_em > now() - interval '7 days'
            AND s.lucro_pct = (
                SELECT max(lucro_pct)
                FROM spreads
                WHERE simbolo = s.simbolo
                AND date_trunc('hour', coletado_em) = date_trunc('hour', s.coletado_em)
            )
            ORDER BY s.coletado_em DESC
            """, nativeQuery = true)
    List<Object[]> buscarHistorico7Dias();
}
