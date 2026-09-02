package com.personalProject.CriptoAnaliser.model;


import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Getter
@Table(name="precos")
public class Preco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coleta_id")
    private UUID coletaId;

    @Column(name = "coletado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime coletadoEm;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String simbolo;

    @Column(name = "melhor_compra", nullable = false, precision = 24, scale = 10)
    private BigDecimal melhorCompra;

    @Column(name = "melhor_venda", nullable = false, precision = 24, scale = 10)
    private BigDecimal melhorVenda;

    @Column(name = "taxa_taker", nullable = false, precision = 10, scale = 6)
    private BigDecimal taxaTaker;

}
