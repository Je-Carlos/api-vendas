package com.example.vendas_service.services;

import com.example.vendas_service.dto.ProdutoDTO;
import com.example.vendas_service.models.Venda;
import com.example.vendas_service.repository.VendasRepository;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class VendaService {
    private final VendasRepository repository;
    private final WebClient products;

    public VendaService(VendasRepository repository, WebClient.Builder webClient,
                        @Value("${produtos.base-url}") String productsUrl,
                        @Value("${produtos.timeout-seconds:3}") long timeoutSeconds) {
        this.repository = repository;
        this.products = webClient.baseUrl(productsUrl).build();
        this.productsTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    private final Duration productsTimeout;

    public Mono<Venda> salvarVenda(Long produtoId, int quantidade) {
        return products.get().uri("/produtos/{id}", produtoId).retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado")))
                .onStatus(status -> status.isError(),
                        response -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Produto indisponivel")))
                .bodyToMono(ProdutoDTO.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta vazia de produtos")))
                .timeout(productsTimeout)
                .onErrorMap(TimeoutException.class,
                        error -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Produto indisponivel"))
                .onErrorMap(WebClientRequestException.class,
                        error -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Produto indisponivel"))
                .onErrorMap(DecodingException.class,
                        error -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta invalida de produtos"))
                .flatMap(produto -> {
                    if (produto.getId() == null || !produto.getId().equals(produtoId)
                            || produto.getPreco() == null || !Double.isFinite(produto.getPreco())
                            || produto.getPreco() < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "Resposta invalida de produtos"));
                    }
                    Venda venda = new Venda();
                    venda.setIdProduto(produto.getId());
                    venda.setQuantidade(quantidade);
                    venda.setValorProduto(produto.getPreco());
                    return repository.save(venda);
                });
    }
}
