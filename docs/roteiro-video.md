# Roteiro de gravação do vídeo

Duração-alvo: 2min50s. O limite da atividade é de 3 minutos.

## Antes de gravar

1. Envie as branches ao GitHub, porque a última cena mostra o repositório.
2. Confirme que o MongoDB local está em execução e inicie a aplicação com o JDK 21.
3. Entre em `http://localhost:8080/dashboard` com um usuário GERENTE ou ADMIN.
4. Deixe abertas, nesta ordem, as abas:
   - `http://localhost:8080/dashboard`
   - `http://localhost:8080/api/dashboard/resumo`
   - `docs/requisitos.md` no GitHub, na seção "Principais decisões de projeto"
   - `docs/evidencias-testes.md` no GitHub, na tabela "Resumo"
5. Use o navegador em tela cheia, com zoom de 90%, e silencie as notificações.
6. Se gravar depois de 09/11/2026, informe 09/11/2026 como data final e use o início 30 dias antes, para o gráfico não terminar em dias sem dados de demonstração.
7. Faça um ensaio cronometrado. Para gravar, use `Win + Alt + R`, da Xbox Game Bar do Windows, ou o OBS Studio.

## Cenas

| Tempo | Ação na tela | Fala |
|---|---|---|
| 0:00 a 0:15 | Painel parado, no topo | "Este é o Dashboard Administrativo do Pediu Patrão. Ele permite ao responsável pela pizzaria acompanhar vendas, pedidos e tendências da operação, usando uma API que eu projetei e implementei." |
| 0:15 a 0:40 | Passar o mouse pelos seis indicadores | "No topo estão os indicadores do período: faturamento, pedidos válidos, ticket médio, itens vendidos, cancelamentos e pedidos em andamento. Faturamento e pedidos são comparados com o período anterior de mesma duração. Assim, o gestor vê se a operação melhorou, e pedidos cancelados não entram no faturamento." |
| 0:40 a 1:05 | Trocar "Por dia" para "Por semana", voltar para "Por dia" e descer até "Pedidos por horário" | "Este gráfico mostra a evolução do faturamento por dia, semana ou mês. Aqui embaixo vemos os horários de pico, que ajudam a organizar a equipe, e ao lado a participação de cada tipo de produto no faturamento." |
| 1:05 a 1:20 | Descer pelo ranking e pela lista e clicar em "Próxima" | "O painel também mostra a situação dos pedidos, os produtos mais vendidos e a lista paginada de pedidos do período." |
| 1:20 a 1:45 | Voltar ao topo. Selecionar o tipo "Bebida" e clicar em "Aplicar". Depois informar 20/10/2026 e 01/10/2026 e clicar em "Aplicar". Clicar em "Limpar" | "Os filtros valem para a tela inteira. Filtrando bebidas, os valores consideram só esses itens. Se eu informar um período inválido, a API responde com uma mensagem clara e os dados continuam na tela. O painel também se atualiza a cada sessenta segundos." |
| 1:45 a 2:05 | Ir para a aba `/api/dashboard/resumo` | "Todos esses dados vêm da API, com oito endpoints de consulta integrados ao MongoDB. Só administradores e gerentes têm acesso, e os erros seguem um formato único, documentado com parâmetros e exemplos." |
| 2:05 a 2:40 | Ir para a aba de decisões de projeto | "Na análise, vi que os pedidos já tinham os dados necessários, então não criei coleções novas. O banco filtra o período e o Java faz os cálculos, porque os valores monetários são gravados como texto no MongoDB. Reaproveitei as regras de status e de pedidos antigos, e usei o fuso de São Paulo para um pedido da noite não cair no dia seguinte." |
| 2:40 a 2:55 | Ir para a aba de evidências | "Os testes cobrem perfis de acesso, erros, conferência dos totais e banco indisponível: vinte e nove casos na API e doze na tela, todos aprovados. A implementação está em branch no modelo gitflow. Obrigado." |

## Se o tempo estourar

Encurte primeiro a cena da lista de pedidos (1:05 a 1:20). Mantenha a cena de decisões de projeto, porque ela é exigida na atividade.
