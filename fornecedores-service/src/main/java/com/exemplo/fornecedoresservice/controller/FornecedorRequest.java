package com.exemplo.fornecedoresservice.controller;

import jakarta.validation.constraints.NotBlank;

public record FornecedorRequest(@NotBlank String nome, @NotBlank String cnpj) {}
