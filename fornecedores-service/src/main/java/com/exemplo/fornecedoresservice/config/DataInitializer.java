package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final FornecedorRepository repository;

    public DataInitializer(FornecedorRepository repository) { this.repository = repository; }

    @Override
    public void run(String... args) {
        repository.save(new Fornecedor("Fornecedor Alfa", "10000000000001"));
        repository.save(new Fornecedor("Fornecedor Beta", "10000000000002"));
        repository.save(new Fornecedor("Fornecedor Gama", "10000000000003"));
        repository.save(new Fornecedor("Fornecedor Delta", "10000000000004"));
        repository.save(new Fornecedor("Fornecedor Epsilon", "10000000000005"));
    }
}
