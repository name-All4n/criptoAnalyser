package com.personalProject.CriptoAnaliser.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "spreads")
@Getter
public class Spread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coleta_id")
    private UUID coletaId;

    @Column(name = "coletado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime coletadoEm;

    @Column(nullable = false)
    private String simbolo;

    @Column(name = "volume_usdt", nullable = false, precision = 20, scale = 2)
    private BigDecimal volumeUsdt;

    @Column(name = "exchange_compra", nullable = false)
    private String exchangeCompra;

    @Column(name = "exchange_venda", nullable = false)
    private String exchangeVenda;

    @Column(name = "lucro_pct", nullable = false, precision = 10, scale = 6)
    private BigDecimal lucroPct;

}
