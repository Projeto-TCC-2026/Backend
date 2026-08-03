# Guia de Teste Manual - Sistema TCC

## Status Atual
✅ **Backend**: Rodando em http://localhost:8081  
✅ **Frontend**: Rodando em http://localhost:4200  
✅ **Banco H2**: Console em http://localhost:8081/h2-console  

## 1. Configuração de Dados de Teste

### 1.1 Acessar Console H2
1. Abra: http://localhost:8081/h2-console
2. Configure a conexão:
   - **JDBC URL**: `jdbc:h2:mem:testdb`
   - **User Name**: `sa`
   - **Password**: (deixe em branco)
3. Clique em "Connect"

### 1.2 Executar Script de Dados de Teste
1. No console H2, execute o conteúdo do arquivo `test_data.sql`
2. Isso criará:
   - 2 hospitais de exemplo
   - 2 usuários (admin e doutor)
   - 5 pacientes de exemplo

## 2. Usuários de Teste

### 2.1 Administrador
- **Email**: `admin@tcc.com`
- **Senha**: `123456`
- **Permissões**: Acesso completo a pacientes (criar, visualizar, inativar, excluir)

### 2.2 Doutor
- **Email**: `doutor@tcc.com`
- **Senha**: `123456`
- **Permissões**: Gerenciar pacientes (criar, visualizar, inativar) - não pode excluir

## 3. Testes da Tela de Pacientes

### 3.1 Login
1. Acesse: http://localhost:4200
2. Faça login com um dos usuários de teste
3. Navegue para "Pacientes" no menu

### 3.2 Funcionalidades a Testar

#### Listagem
- [ ] Lista mostra os 5 pacientes cadastrados
- [ ] Paginação funciona (se houver mais de 10 registros)
- [ ] Status "Ativo" é exibido corretamente

#### Busca
- [ ] Busca por nome: digite "Maria" e teste
- [ ] Busca por CPF: digite "123" e teste  
- [ ] Busca por email: digite "email.com" e teste
- [ ] Busca por telefone: digite "11" e teste

#### Filtros Avançados
- [ ] Filtro por gênero: selecione "Feminino"
- [ ] Filtro por cidade: digite "São Paulo"
- [ ] Filtro por estado: digite "SP"
- [ ] Combinação de filtros múltiplos

#### Visualização
- [ ] Clique no ícone do olho para visualizar detalhes
- [ ] Modal mostra informações completas do paciente
- [ ] Dados pessoais, contato e endereço são exibidos

#### Criação (ADMIN e DOCTOR)
- [ ] Clique em "Novo Paciente"
- [ ] Preencha o formulário com dados válidos
- [ ] Teste validações (CPF inválido, etc.)
- [ ] Salve e verifique se aparece na lista

#### Inativação (ADMIN e DOCTOR)
- [ ] Clique no ícone X para inativar
- [ ] Confirme a ação
- [ ] Verifique se status mudou para "Inativo"

#### Exclusão (apenas ADMIN)
- [ ] Como ADMIN: ícone de lixeira deve aparecer
- [ ] Como DOCTOR: ícone de lixeira não deve aparecer
- [ ] Teste a exclusão com confirmação

### 3.3 Controle de Acesso por Role

#### Como ADMIN
- [ ] Pode ver botão "Novo Paciente"
- [ ] Pode visualizar pacientes
- [ ] Pode inativar pacientes
- [ ] Pode excluir pacientes (ícone lixeira visível)

#### Como DOCTOR  
- [ ] Pode ver botão "Novo Paciente"
- [ ] Pode visualizar pacientes
- [ ] Pode inativar pacientes
- [ ] NÃO pode excluir (ícone lixeira não aparece)

#### Como HOSPITAL (se implementado)
- [ ] NÃO deve conseguir acessar tela de pacientes

## 4. Testes de API (Opcional - Via Postman/Insomnia)

### 4.1 Autenticação
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "admin@tcc.com",
  "password": "123456"
}
```

### 4.2 Listar Pacientes (com token)
```
GET http://localhost:8081/api/patients
Authorization: Bearer [TOKEN_AQUI]
```

## 5. Problemas Conhecidos e Soluções

### Backend não inicia
- Verifique se porta 8081 está livre
- Examine logs do console para erros
- Reinicie com: `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev --server.port=8081"`

### Frontend não carrega
- Verifique se porta 4200 está livre  
- Reinicie com: `npm start`
- Verifique se `environment.ts` aponta para porta 8081

### Erro 500 na API
- Verifique se dados de teste foram inseridos
- Examine logs do backend
- Teste endpoints básicos primeiro

### Problemas de CORS
- Backend está configurado para aceitar requisições do frontend
- Se persistir, verifique configuração de CORS no backend

## 6. Próximos Passos

Após os testes, você pode:
1. Implementar mais funcionalidades
2. Adicionar mais validações
3. Melhorar a interface
4. Implementar testes automatizados
5. Configurar ambiente de produção

## Notas Importantes

- Dados são perdidos ao reiniciar (banco em memória)
- Para dados persistentes, configure PostgreSQL
- Senhas estão em hash BCrypt para segurança
- Tokens JWT têm validade de 15 minutos (configurável)