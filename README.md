# Pediu Patrão

Aplicação web para cadastro de clientes e produtos e gerenciamento de pedidos de uma pizzaria. Este projeto foi desenvolvido como atividade prática individual de Engenharia de Software, com autenticação, autorização por perfil, controle do fluxo dos pedidos e auditoria persistida.

## Tecnologias

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Data MongoDB
- Thymeleaf
- Maven

## Requisitos para executar

- JDK 21 ou compatível com a versão configurada no projeto.
- MongoDB local ou uma instância MongoDB acessível pela aplicação.
- PowerShell no Windows, ou terminal equivalente para outros sistemas.

Por padrão, a aplicação usa `mongodb://localhost:27017/pizzaria`. Para apontar para outra instância, defina `MONGODB_URI` antes de iniciar.

No PowerShell:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/pizzaria"
```

## Executar a aplicação

Na pasta raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse [http://localhost:8080](http://localhost:8080). O inicializador cria um usuário `admin` com perfil ADMIN caso ainda não exista; a senha de desenvolvimento está definida em [`DataInitializer.java`](src/main/java/com/umc/pediupatrao/config/DataInitializer.java). Essa conta é apenas para desenvolvimento e demonstração: altere a credencial antes de qualquer uso fora de ambiente local.

Com a conta ADMIN, cadastre usuários para os perfis GERENTE e ATENDENTE. Em seguida, cadastre produtos e clientes para testar a criação dos pedidos.

## Testes automatizados

Execute a suíte Maven:

```powershell
.\mvnw.cmd test
```

Os testes automatizados cobrem cálculos e itens de pedidos, valores financeiros recalculados no servidor e registros de auditoria para operações de pedidos e clientes. A suíte não substitui a verificação manual das telas, dos perfis e das tentativas de acesso não autorizado.

## Requisitos funcionais

### RF01 — Controle de usuários e permissões

Os perfis reconhecidos são ADMIN, GERENTE e ATENDENTE. A autorização é aplicada no backend:

- Somente o GERENTE pode aplicar descontos, cancelar pedidos e alterar o status dos pedidos.
- ADMIN e GERENTE podem gerenciar produtos.
- ATENDENTE pode cadastrar e alterar clientes; GERENTE pode consultá-los.
- O gerenciamento de usuários é reservado ao ADMIN.
- A alteração do próprio username/perfil e a exclusão da própria conta pelo ADMIN são bloqueadas.
- A proteção CSRF permanece habilitada, inclusive para operações autenticadas da API.

As regras principais estão em [`SecurityConfig.java`](src/main/java/com/umc/pediupatrao/config/SecurityConfig.java) e [`UsuarioService.java`](src/main/java/com/umc/pediupatrao/service/UsuarioService.java).

### RF02 — Evolução do cadastro de pedidos

Um pedido pode conter vários produtos, cada um com sua quantidade. Os preços unitários são obtidos do catálogo no servidor; subtotais, desconto e total são calculados pela aplicação. Valores financeiros recebidos na requisição não definem o total. O registro inicial também recebe no servidor o usuário autenticado e a data/hora de entrada.

Implementação principal: [`Pedido.java`](src/main/java/com/umc/pediupatrao/entity/Pedido.java), [`ItemPedido.java`](src/main/java/com/umc/pediupatrao/entity/ItemPedido.java) e [`PedidoService.java`](src/main/java/com/umc/pediupatrao/service/PedidoService.java).

### RF03 — Controle de entrada, saída e transições dos pedidos

O fluxo de status é validado no serviço: RECEBIDO, EM PREPARAÇÃO, PRONTO, SAIU PARA ENTREGA ou RETIRADO e FINALIZADO. A entrada e a saída registram data/hora e responsável. Transições inválidas e alterações posteriores ao cancelamento são bloqueadas.

Implementação principal: [`PedidoService.java`](src/main/java/com/umc/pediupatrao/service/PedidoService.java).

### RF04 — Cancelamentos e descontos

O cancelamento é exclusivo do GERENTE, exige justificativa e só é permitido antes da saída para entrega ou retirada. O desconto também é exclusivo do GERENTE, limitado a 20% do pedido e permitido antes da saída. O percentual pode ser alterado ou removido, respeitando o limite.

As operações preservam o pedido e registram os dados de auditoria descritos no RF05. Implementação principal: [`PedidoService.java`](src/main/java/com/umc/pediupatrao/service/PedidoService.java), [`HistoricoStatusPedido.java`](src/main/java/com/umc/pediupatrao/entity/HistoricoStatusPedido.java) e [`HistoricoDescontoPedido.java`](src/main/java/com/umc/pediupatrao/entity/HistoricoDescontoPedido.java).

### RF05 — Auditoria das alterações de pedidos e clientes

Os registros de auditoria são persistidos na coleção MongoDB `auditoria`. São registrados inclusões e alterações de pedidos, mudanças de status, descontos, cancelamentos e inclusões e alterações de clientes. Cada registro contém entidade e identificador, operação, usuário, perfil, data/hora e valores anteriores e novos dos dados comerciais ou cadastrais. Justificativas de cancelamento também ficam registradas.

A consulta está disponível em `/auditoria` e `GET /api/auditoria`, restrita a ADMIN e GERENTE. Não há rotas de alteração ou exclusão de registros de auditoria para as interfaces e APIs da aplicação.

Implementação principal: [`RegistroAuditoria.java`](src/main/java/com/umc/pediupatrao/entity/RegistroAuditoria.java), [`RegistroAuditoriaRepository.java`](src/main/java/com/umc/pediupatrao/repository/RegistroAuditoriaRepository.java), [`AuditoriaService.java`](src/main/java/com/umc/pediupatrao/service/AuditoriaService.java) e [`AuditoriaController.java`](src/main/java/com/umc/pediupatrao/controller/AuditoriaController.java).

Os registros começam a ser produzidos depois da implementação da auditoria; ações anteriores não são reconstruídas retroativamente. A tela/API de auditoria destina-se à consulta autorizada dos dados necessários à atividade.

## Principais áreas do código

- `src/main/java/com/umc/pediupatrao/entity`: entidades e documentos MongoDB.
- `src/main/java/com/umc/pediupatrao/repository`: repositórios Spring Data.
- `src/main/java/com/umc/pediupatrao/service`: regras de negócio e autorização por método.
- `src/main/java/com/umc/pediupatrao/controller`: rotas web e API.
- `src/main/resources/templates`: páginas Thymeleaf.
- `src/test/java`: testes automatizados.