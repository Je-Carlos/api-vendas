package com.example.vendas_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.vendas_service.models.Venda;

public interface VendasRepository extends ReactiveCrudRepository<Venda, Long> {}
