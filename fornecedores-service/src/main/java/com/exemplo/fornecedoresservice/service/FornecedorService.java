package com.exemplo.fornecedoresservice.service;

import com.exemplo.fornecedoresservice.client.ProdutoClient;
import com.exemplo.fornecedoresservice.client.ProdutoDTO;
import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FornecedorService {
    private final FornecedorRepository repository;
    private final ProdutoClient produtos;

    public FornecedorService(FornecedorRepository repository, ProdutoClient produtos) {
        this.repository = repository;
        this.produtos = produtos;
    }

    public List<Fornecedor> listar() { return repository.findAll(); }
    public Optional<Fornecedor> buscar(Long id) { return repository.findById(id); }
    public Fornecedor cadastrar(String nome, String cnpj) {
        return repository.saveAndFlush(new Fornecedor(nome, cnpj));
    }
    public List<ProdutoDTO> produtos() { return produtos.listar(); }
}
