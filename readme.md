# API de vendas

Autor: JEAN CARLOS COTILLO BERG.

Projeto de microsserviços em Java 17, Spring Boot 3.2.5 e Spring Cloud 2023.0.1. O único ponto de entrada publicado pelo Docker Compose é `http://localhost:8085`.

## Arquitetura

| Serviço | Porta interna | Responsabilidade | Web e persistência | Integração e segurança |
| --- | ---: | --- | --- | --- |
| gateway | 8085 | Entrada e roteamento via Eureka | WebFlux, sem banco | Valida JWT HS256; só login, cadastro, refresh e health são públicos |
| auth-service | 8084 | Usuários, login e refresh | Spring MVC, Spring Data JDBC, PostgreSQL `authdb` | Emite JWT e refresh token |
| vendas-service | 8082 | Criação de vendas | WebFlux, Spring Data R2DBC, PostgreSQL `vendasdb` | Valida JWT; busca produto com WebClient |
| produtos-service | 8081 | Catálogo de produtos | Spring MVC, JPA, H2 próprio em memória | Fornece `GET /produtos/{id}` a vendas |
| clientes-service | 8083 | Consulta de clientes | Spring MVC, JPA, H2 próprio em memória | Rota via Eureka |
| fornecedores-service | 8084 | Cadastro de fornecedores e consulta de produtos | Spring MVC, JPA, H2 próprio em memória | OpenFeign consulta produtos via Eureka |
| eureka-server | 8761 | Descoberta de serviços | Sem banco | Uso interno |
| config-server | 8888 | Configuração local de `config-repo` | Sem banco | Uso interno |

`authdb` pertence exclusivamente ao auth-service e `vendasdb` exclusivamente ao vendas-service. Seus containers, volumes, credenciais e esquemas são separados. Produtos, clientes e fornecedores mantêm bancos H2 próprios, efêmeros. Nenhum serviço acessa as tabelas de outro: vendas e fornecedores consultam produtos por HTTP.

## Endpoints do gateway

| Método | Caminho em `localhost:8085` | Autenticação | Resultado |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Pública | Cadastra usuário (`nome`, `email`, `senha`), devolve id e 201 |
| POST | `/api/auth/login` | Pública | Troca email e senha por access e refresh tokens |
| POST | `/api/auth/refresh` | Pública com refresh token no corpo | Rotaciona o refresh token |
| POST | `/api/vendas` | Bearer JWT | Cria venda (`idProduto`, `quantidade`) |
| GET | `/produtos-service/produtos` ou `/produtos-service/produtos/{id}` | Bearer JWT | Rotas descobertas via Eureka |
| GET | `/clientes-service/clientes` | Bearer JWT | Rota descoberta via Eureka |
| GET | `/fornecedores-service/fornecedores` ou `/fornecedores-service/fornecedores/{id}` | Bearer JWT | Lista ou consulta fornecedor; ID ausente retorna 404 |
| POST | `/fornecedores-service/fornecedores` | Bearer JWT | Cadastra `nome` e `cnpj`; retorna 201 e ID gerado |
| GET | `/fornecedores-service/fornecedores/produtos` | Bearer JWT | Lista produtos consultados por OpenFeign |
| GET | `/actuator/health` | Pública | Saúde do gateway |

As rotas explícitas `/api/auth/**` e `/api/vendas/**` encaminham o caminho integral, sem reescrita. As rotas com prefixo do nome do serviço são geradas pelo localizador do Gateway e removem esse prefixo antes do encaminhamento. Os demais endpoints e serviços só são acessíveis na rede interna do Compose.

## Autenticação

O auth-service guarda senhas com BCrypt em PostgreSQL via Spring Data JDBC. O login devolve `accessToken`, `refreshToken`, `tokenType: "Bearer"` e `expiresIn: 900` (segundos por padrão). O JWT HS256 contém `sub` (email), `iat`, `exp` e `roles: "USER"`; gateway e vendas verificam assinatura e validade com o mesmo `JWT_SECRET`. O segredo só vem do ambiente, nunca do repositório.

O refresh token é opaco e aleatório, com duração padrão de 7 dias. O banco guarda apenas seu SHA-256. Cada uso válido revoga o token anterior e cria outro, na mesma transação; repetição, token desconhecido ou expirado retorna 401. Requisição inválida retorna 400, email duplicado retorna 409 e credenciais incorretas retornam 401, sem ecoar segredos. Os tempos podem ser configurados em segundos com `ACCESS_TOKEN_TTL_SECONDS` e `REFRESH_TOKEN_TTL_SECONDS`.

## Execução

Requisitos: Docker Desktop/Engine com Docker Compose e portas 8085 livre. Para compilar fora dos containers, use JDK 17 ou superior e Maven 3.9. Testcontainers exige o daemon Docker acessível ao processo Java.

```sh
cp .env.example .env
# Edite .env: gere um JWT_SECRET aleatório de pelo menos 32 caracteres
# e escolha senhas distintas para AUTH_DB_PASSWORD e VENDAS_DB_PASSWORD.
docker compose up --build -d
docker compose ps
curl http://localhost:8085/actuator/health
```

O Compose só publica `8085`. Os bancos têm volumes persistentes nomeados. Para encerrar sem apagar dados: `docker compose down`. Para acompanhar a inicialização: `docker compose logs -f auth-service vendas-service gateway`. O Config Server usa os arquivos em `config-repo`; o Compose aguarda os serviços e bancos ficarem saudáveis antes de iniciar seus dependentes.

Ao reutilizar volumes PostgreSQL, mantenha as senhas usadas quando foram criados. Para testar com credenciais novas sem alterar os dados existentes, use um nome de projeto isolado, por exemplo `docker compose -p fornecedores-todo up --build -d --wait`.

Fornecedores inicia com cinco registros no H2 e atende internamente na porta `8084`. Auth também usa `8084` em outro contêiner, sem conflito. Fora do Docker, inicie os dois separadamente nessa porta. O H2 Console local fica em `http://localhost:8084/h2-console` (JDBC URL `jdbc:h2:mem:fornecedoresdb`).

## Demonstração com curl

```sh
curl -i -X POST http://localhost:8085/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@example.com","senha":"secret123"}'

curl -i -X POST http://localhost:8085/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","senha":"secret123"}'

# Copie accessToken e refreshToken da resposta do login.
curl -i http://localhost:8085/produtos-service/produtos/1
curl -i http://localhost:8085/produtos-service/produtos/1 \
  -H 'Authorization: Bearer ACCESS_TOKEN'
curl -i -X POST http://localhost:8085/api/vendas \
  -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/json' \
  -d '{"idProduto":1,"quantidade":2}'
curl -i -X POST http://localhost:8085/api/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"REFRESH_TOKEN"}'
# Repita o mesmo refreshToken para verificar o 401; use o novo accessToken na venda.
curl -i -X POST http://localhost:8085/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","senha":"senha-errada"}'

curl -i http://localhost:8085/fornecedores-service/fornecedores \
  -H 'Authorization: Bearer ACCESS_TOKEN'
curl -i -X POST http://localhost:8085/fornecedores-service/fornecedores \
  -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/json' \
  -d '{"nome":"Fornecedor Teste","cnpj":"12345678000199"}'
curl -i http://localhost:8085/fornecedores-service/fornecedores/produtos \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

Os dados iniciais de produtos incluem o id `1`. Em caso de indisponibilidade ou demora maior que 3 segundos do serviço de produtos, a venda retorna 502; produto ausente retorna 404. O limite pode ser ajustado por `PRODUTOS_TIMEOUT_SECONDS`. Vendas exige quantidade e id de produto positivos.

## Testes

Com Docker acessível, execute:

```sh
mvn -f auth-service/pom.xml test
mvn -f gateway/pom.xml test
mvn -f vendas-service/pom.xml test
mvn -f fornecedores-service/pom.xml test
docker compose config
```

Com o Compose ativo, `pwsh fornecedores-service/eval.ps1` verifica cadastro, JWT, rotas, Eureka e Config Server. Se usar `-p fornecedores-todo`, defina `$env:COMPOSE_PROJECT_NAME='fornecedores-todo'` antes da avaliação. O workflow GitHub Actions compila e testa apenas `fornecedores-service` com Java 17 em cada push.

Os testes de auth exercitam JWT, cadastro, login, limite BCrypt de 72 bytes UTF-8, persistência JDBC, rotação e replay com PostgreSQL Testcontainers. Os de vendas usam WebTestClient, StepVerifier e PostgreSQL Testcontainers para verificar autorização, timeout da consulta de produtos e persistência R2DBC. O teste do gateway verifica 401 em JSON. Produtos e clientes preservam suas configurações atuais.
