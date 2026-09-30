package com.example.vendas_service.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vendas_service.dto.VendaDTO;
import com.example.vendas_service.models.Venda;
import com.example.vendas_service.services.VendaService;
import reactor.core.publisher.Mono;



@RestController
@RequestMapping("/api/vendas")
public class VendaController {
    
    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<VendaDTO> salvarVenda(@Valid @RequestBody VendaDTO vendaDTO) {
        return service.salvarVenda(vendaDTO.getIdProduto(), vendaDTO.getQuantidade())
                .map(saved -> new VendaDTO(saved.getId(), saved.getIdProduto(),
                        saved.getQuantidade(), saved.getValorProduto()));
    }

}
