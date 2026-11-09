# Roteiro do vídeo de apresentação

Duração prevista: até 3 minutos. O painel deve estar aberto com o login de GERENTE e os dados de demonstração carregados.

| Tempo | Na tela | Fala |
|---|---|---|
| 0:00 a 0:20 | Painel, visão geral | "Este é o Dashboard Administrativo do Pediu Patrão. Ele ajuda o responsável pela pizzaria a acompanhar as vendas, os pedidos e as tendências da operação, com dados de uma API que eu projetei e implementei." |
| 0:20 a 0:45 | Indicadores | "No topo estão os indicadores: faturamento, pedidos válidos, ticket médio, itens vendidos, cancelamentos e pedidos em andamento. Faturamento e pedidos são comparados com o período anterior de mesma duração, para mostrar se a operação melhorou." |
| 0:45 a 1:15 | Gráficos: trocar para "Por semana" e mostrar horários | "O gráfico principal mostra a evolução do faturamento por dia, semana ou mês. O de horários mostra os picos, útil para organizar a equipe. Aqui ao lado vemos a participação de cada tipo de produto." |
| 1:15 a 1:35 | Status, ranking e lista | "Também acompanho a situação dos pedidos, os produtos mais vendidos e a lista paginada do período." |
| 1:35 a 2:00 | Aplicar o filtro Bebida, depois inverter as datas | "Os filtros valem para a tela inteira. Filtrando bebidas, o faturamento considera só esses itens. Se eu informar um período inválido, a API devolve uma mensagem clara e os dados continuam na tela." |
| 2:00 a 2:40 | `docs/api-dashboard.md` e `docs/requisitos.md` | "Na análise, identifiquei que os pedidos já tinham os dados necessários, então não criei coleções novas. O banco filtra o período, e os cálculos ficam no Java, porque os valores em dinheiro são gravados como texto no MongoDB. Reaproveitei as regras de status e de pedidos antigos, e o acesso é restrito a administradores e gerentes. Também usei o fuso de São Paulo, para um pedido à noite não cair no dia seguinte." |
| 2:40 a 3:00 | `docs/evidencias-testes.md` e branches no GitHub | "A entrega inclui requisitos, documentação dos endpoints e evidências de testes, com perfis de acesso, erros, totais e até o banco fora do ar. A implementação foi feita em branch no modelo gitflow. Obrigado." |
