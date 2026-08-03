# Script PowerShell para criar dados de teste via H2 Console
# Execute este script após iniciar o backend

Write-Host "=== Criando Dados de Teste ===" -ForegroundColor Green

# Verificar se backend está rodando
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "Backend esta rodando na porta 8081" -ForegroundColor Green
    }
} catch {
    Write-Host "Backend nao esta rodando. Inicie primeiro com: ./mvnw spring-boot:run" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Para criar dados de teste:" -ForegroundColor Yellow
Write-Host "1. Abra: http://localhost:8081/h2-console" -ForegroundColor White
Write-Host "2. Configure:" -ForegroundColor White
Write-Host "   - JDBC URL: jdbc:h2:mem:testdb" -ForegroundColor Cyan
Write-Host "   - User: sa" -ForegroundColor Cyan
Write-Host "   - Password: (vazio)" -ForegroundColor Cyan
Write-Host "3. Execute o conteudo do arquivo test_data.sql" -ForegroundColor White
Write-Host ""

Write-Host "URLs Importantes:" -ForegroundColor Yellow
Write-Host "   - Frontend: http://localhost:4200" -ForegroundColor Cyan
Write-Host "   - Backend:  http://localhost:8081" -ForegroundColor Cyan
Write-Host "   - H2 Console: http://localhost:8081/h2-console" -ForegroundColor Cyan
Write-Host ""

Write-Host "Usuarios de Teste:" -ForegroundColor Yellow
Write-Host "   - ADMIN: admin@tcc.com / 123456" -ForegroundColor Cyan
Write-Host "   - DOCTOR: doutor@tcc.com / 123456" -ForegroundColor Cyan
Write-Host ""

Write-Host "Consulte GUIA_TESTE_MANUAL.md para instrucoes completas" -ForegroundColor Green