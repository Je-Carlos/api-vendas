package com.exemplo.fornecedoresservice.controller;

import com.exemplo.fornecedoresservice.client.ProdutoDTO;
import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.service.FornecedorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {
    private final FornecedorService service;

    public FornecedorController(FornecedorService service) { this.service = service; }

    @GetMapping
    public List<Fornecedor> listar() { return service.listar(); }

    @GetMapping("/produtos")
    public List<ProdutoDTO> produtos() { return service.produtos(); }

    @GetMapping("/{id}")
    public ResponseEntity<Fornecedor> buscar(@PathVariable Long id) {
        return service.buscar(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Fornecedor> cadastrar(@Valid @RequestBody FornecedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.cadastrar(request.nome(), request.cnpj()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> duplicado() { return ResponseEntity.status(HttpStatus.CONFLICT).build(); }
}
