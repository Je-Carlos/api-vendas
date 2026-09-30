package com.exemplo.fornecedoresservice.client;

import java.math.BigDecimal;

public record ProdutoDTO(Long id, String nome, BigDecimal preco) {}
