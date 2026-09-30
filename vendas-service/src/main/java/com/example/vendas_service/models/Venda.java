package com.example.vendas_service.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("vendas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Venda {

    @Id
    private Long id;

    @Column("id_produto")
    private Long idProduto;

    private Integer quantidade;

    @Column("valor_produto")
    private Double valorProduto;
}
