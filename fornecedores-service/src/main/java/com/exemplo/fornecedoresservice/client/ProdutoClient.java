package com.exemplo.fornecedoresservice.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "produtos-service")
public interface ProdutoClient {
    @GetMapping("/produtos")
    List<ProdutoDTO> listar();
}
