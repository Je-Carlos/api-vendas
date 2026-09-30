package com.example.vendas_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendaDTO {
    private Long id;
    @NotNull @Positive private Long idProduto;
    @NotNull @Positive private Integer quantidade;
    private Double preco;
}
