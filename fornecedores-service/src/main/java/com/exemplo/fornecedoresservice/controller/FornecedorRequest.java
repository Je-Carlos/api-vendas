package com.exemplo.fornecedoresservice.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(@NotBlank @Size(max = 255) String nome,
                               @NotBlank @Size(max = 255) String cnpj) {}
