package com.exemplo.fornecedoresservice.repository;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {}
