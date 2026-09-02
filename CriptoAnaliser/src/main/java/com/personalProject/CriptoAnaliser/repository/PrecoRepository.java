package com.personalProject.CriptoAnaliser.repository;

import com.personalProject.CriptoAnaliser.model.Preco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrecoRepository extends JpaRepository<Preco,Long> {
    @Query(value = """
            SELECT * FROM precos
            WHERE coleta_id = (
                SELECT coleta_id FROM precos
                WHERE coleta_id IS NOT NULL
                ORDER BY coletado_em DESC
                LIMIT 1
            )
            ORDER BY simbolo, exchange
            """, nativeQuery = true)
    List<Preco> buscarUltimaColeta();
}
