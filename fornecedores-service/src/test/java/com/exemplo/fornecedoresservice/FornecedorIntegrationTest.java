package com.exemplo.fornecedoresservice;

import com.exemplo.fornecedoresservice.client.ProdutoClient;
import com.exemplo.fornecedoresservice.client.ProdutoDTO;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:fornecedorestest",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class FornecedorIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FornecedorRepository repository;
    @MockBean ProdutoClient produtos;

    @Test
    void listaCincoFornecedoresEBuscaPorId() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(5, repository.count());
        mvc.perform(get("/fornecedores")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get("/fornecedores/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/fornecedores/999")).andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void cadastraEConsultaFornecedor() throws Exception {
        mvc.perform(post("/fornecedores").contentType("application/json")
                        .content("{\"nome\":\"Fornecedor Teste\",\"cnpj\":\"12345678000199\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(get("/fornecedores")).andExpect(jsonPath("$", hasSize(6)));
        Long id = repository.findAll().stream().filter(f -> "12345678000199".equals(f.getCnpj()))
                .findFirst().orElseThrow().getId();
        mvc.perform(get("/fornecedores/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fornecedor Teste"));
    }

    @Test
    void rejeitaNomeECnpjVazios() throws Exception {
        mvc.perform(post("/fornecedores").contentType("application/json")
                .content("{\"nome\":\"\",\"cnpj\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaCnpjDuplicado() throws Exception {
        mvc.perform(post("/fornecedores").contentType("application/json")
                .content("{\"nome\":\"Duplicado\",\"cnpj\":\"10000000000001\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void consultaProdutosViaFeign() throws Exception {
        when(produtos.listar()).thenReturn(List.of(new ProdutoDTO(1L, "Notebook", new BigDecimal("3500.00"))));
        mvc.perform(get("/fornecedores/produtos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Notebook"));
    }
}
