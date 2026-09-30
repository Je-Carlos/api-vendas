$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8085'
# ponytail: aguarda até 60s pelo primeiro refresh do Eureka; aumente se o ambiente iniciar mais devagar.
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $probe = Invoke-WebRequest "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body '{"email":"probe@example.com","senha":"senha123456"}' -SkipHttpErrorCheck
    if ($probe.StatusCode -eq 401) { break }
    if ($probe.StatusCode -ne 503) { throw "Auth retornou $($probe.StatusCode) durante a espera" }
    Start-Sleep -Seconds 2
}
if ($probe.StatusCode -ne 401) { throw 'Gateway não descobriu auth-service em 60s' }
$email = "fornecedor-$([guid]::NewGuid().ToString('N'))@example.com"
$password = 'senha123456'
$register = @{ nome = 'Teste Fornecedores'; email = $email; senha = $password } | ConvertTo-Json
$response = Invoke-WebRequest "$base/api/auth/register" -Method Post -ContentType 'application/json' -Body $register
if ($response.StatusCode -ne 201) { throw 'Cadastro não retornou 201' }
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = $email; senha = $password } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$unauthorized = Invoke-WebRequest "$base/fornecedores-service/fornecedores" -SkipHttpErrorCheck
if ($unauthorized.StatusCode -ne 401) { throw 'Rota sem JWT não retornou 401' }
$items = Invoke-RestMethod "$base/fornecedores-service/fornecedores" -Headers $headers
if ($items.Count -ne 5) { throw "Esperados 5 fornecedores iniciais; encontrados $($items.Count)" }
$known = Invoke-RestMethod "$base/fornecedores-service/fornecedores/$($items[0].id)" -Headers $headers
if ($known.cnpj -ne $items[0].cnpj) { throw 'Busca por ID retornou outro fornecedor' }
$missing = Invoke-WebRequest "$base/fornecedores-service/fornecedores/999999" -Headers $headers -SkipHttpErrorCheck
if ($missing.StatusCode -ne 404) { throw 'ID inexistente não retornou 404' }
$cnpj = '2' + (Get-Random -Minimum 1000000000000 -Maximum 9999999999999)
$created = Invoke-WebRequest "$base/fornecedores-service/fornecedores" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{ nome = 'Fornecedor Avaliação'; cnpj = $cnpj } | ConvertTo-Json)
if ($created.StatusCode -ne 201 -or -not ($created.Content | ConvertFrom-Json).id) { throw 'POST não criou fornecedor com ID' }
$products = Invoke-RestMethod "$base/fornecedores-service/fornecedores/produtos" -Headers $headers
if ($products.Count -lt 1 -or -not $products[0].nome) { throw 'Feign não retornou produtos' }
$config = docker compose exec -T config-server curl -fsS http://localhost:8888/fornecedores-service/docker
if ($LASTEXITCODE -ne 0 -or $config -notmatch 'server.port' -or $config -notmatch '8084') { throw 'Config Server não forneceu a porta 8084' }
$registry = docker compose exec -T eureka-server curl -fsS -H 'Accept: application/json' http://localhost:8761/eureka/apps/FORNECEDORES-SERVICE
if ($LASTEXITCODE -ne 0 -or $registry -notmatch 'FORNECEDORES-SERVICE') { throw 'Eureka não registrou fornecedores-service' }
'Avaliação funcional de fornecedores: OK'
