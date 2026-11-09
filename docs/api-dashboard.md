# API do Dashboard Administrativo

Base: `http://localhost:8080/api/dashboard`. Todas as rotas usam `GET`, retornam JSON e não alteram dados.

Os exemplos de resposta são de uma execução real, com dados de demonstração. Listas longas foram reduzidas aos primeiros itens.

## Autenticação e perfis

A API aceita a sessão criada pelo login da aplicação ou HTTP Basic. Os perfis autorizados são `ADMIN` e `GERENTE`.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/resumo"
```

## Filtros comuns

Aceitos em todas as rotas, exceto `/filtros`. Todos são opcionais.

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `dataInicio` | data `AAAA-MM-DD` | 29 dias antes de `dataFim` | Início do período, no fuso de São Paulo |
| `dataFim` | data `AAAA-MM-DD` | Dia atual | Fim do período, inclusive |
| `status` | texto | Todos | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` ou `CANCELADO`. Também aceita `PENDENTE`, `EM_PREPARO` e `ENTREGUE` |
| `tipoProduto` | texto | Todos | Tipo do produto, sem diferenciar maiúsculas de minúsculas |
| `produtoId` | texto | Todos | Identificador do produto |

O período máximo é de 366 dias. Com `tipoProduto` ou `produtoId`, os valores consideram apenas os itens correspondentes.

## Formato das respostas

Todas as rotas, exceto `/filtros`, retornam:

| Campo | Descrição |
|---|---|
| `periodo` | Período efetivamente consultado |
| `atualizadoEm` | Momento em que a resposta foi gerada, em UTC |
| `dados` | Conteúdo específico de cada rota |

## Erros

Todos os erros da tabela abaixo usam o mesmo formato JSON.

| Código | Quando ocorre |
|---|---|
| `400 Bad Request` | Parâmetro inválido |
| `401 Unauthorized` | Requisição sem autenticação |
| `403 Forbidden` | Perfil sem permissão |
| `500 Internal Server Error` | Falha inesperada |
| `503 Service Unavailable` | Banco de dados indisponível |

Exemplo de `400`:

```json
{
  "timestamp": "2026-11-09T23:18:13.683047700Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "A data inicial não pode ser posterior à data final.",
  "caminho": "/api/dashboard/resumo"
}
```

Exemplo de `401`:

```json
{
  "timestamp": "2026-11-09T23:17:58.170060700Z",
  "status": 401,
  "erro": "Unauthorized",
  "mensagem": "Autenticação necessária para acessar o dashboard.",
  "caminho": "/api/dashboard/resumo"
}
```

Exemplo de `403`:

```json
{
  "timestamp": "2026-11-09T23:17:58.622227100Z",
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Seu perfil não tem permissão para acessar o dashboard.",
  "caminho": "/api/dashboard/resumo"
}
```

Exemplo de `503`:

```json
{
  "timestamp": "2026-11-09T23:25:38.781411700Z",
  "status": 503,
  "erro": "Service Unavailable",
  "mensagem": "Não foi possível consultar os dados no momento. Tente novamente em instantes.",
  "caminho": "/api/dashboard/resumo"
}
```

Mensagens de `400` por parâmetro:

| Situação | Mensagem |
|---|---|
| Data inicial maior que a final | `A data inicial não pode ser posterior à data final.` |
| Data em formato inválido | `Valor inválido para o parâmetro 'dataInicio'. Datas devem usar o formato AAAA-MM-DD.` |
| Período acima do limite | `O período consultado não pode ultrapassar 366 dias.` |
| Status inexistente | `Status inválido: 'XPTO'. Valores aceitos: RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO.` |
| Agrupamento inexistente | `Agrupamento inválido: 'ANO'. Valores aceitos: DIA, SEMANA, MES.` |
| Limite fora da faixa | `O parâmetro 'limite' deve estar entre 1 e 50.` |
| Página negativa | `O parâmetro 'pagina' não pode ser negativo.` |
| Tamanho fora da faixa | `O parâmetro 'tamanho' deve estar entre 1 e 100.` |

## Endpoints

### GET /resumo

Retorna os indicadores gerais do período e a comparação com o período anterior.

Parâmetros: filtros comuns. Erros: 400, 401, 403, 500 e 503.

Campos de `dados`:

| Campo | Descrição |
|---|---|
| `faturamento` | Soma dos pedidos válidos |
| `totalPedidos` | Pedidos encontrados, incluindo cancelados |
| `pedidosValidos` | Pedidos não cancelados e com status conhecido |
| `pedidosCancelados` | Quantidade de cancelamentos |
| `ticketMedio` | Faturamento dividido pelos pedidos válidos |
| `itensVendidos` | Soma das quantidades vendidas |
| `totalDescontos` | Soma dos descontos dos pedidos válidos |
| `taxaCancelamento` | Percentual de cancelamentos |
| `comparativo` | Valores do período anterior. As variações são percentuais e ficam `null` quando o período anterior não tem base |
| `qualidadeDados` | Pedidos sem data em toda a base, e pedidos do período com status desconhecido ou sem valor |

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/resumo?dataInicio=2026-11-03&dataFim=2026-11-09"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:48.580638600Z",
  "dados": {
    "faturamento": 16755.62,
    "totalPedidos": 118,
    "pedidosValidos": 108,
    "pedidosCancelados": 10,
    "ticketMedio": 155.14,
    "itensVendidos": 467,
    "totalDescontos": 312.38,
    "taxaCancelamento": 8.47,
    "comparativo": {
      "periodoAnterior": {
        "inicio": "2026-10-27",
        "fim": "2026-11-02"
      },
      "faturamentoAnterior": 14025.54,
      "pedidosValidosAnterior": 92,
      "variacaoFaturamento": 19.47,
      "variacaoPedidos": 17.39
    },
    "qualidadeDados": {
      "pedidosSemData": 1,
      "pedidosStatusIndefinido": 0,
      "pedidosSemValor": 0
    }
  }
}
```

### GET /vendas

Retorna a evolução das vendas. Períodos sem vendas aparecem com valor zero.

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| Filtros comuns | | | |
| `agrupamento` | texto | `DIA` | `DIA`, `SEMANA` (a partir de segunda-feira) ou `MES` |

Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/vendas?dataInicio=2026-11-03&dataFim=2026-11-09&agrupamento=DIA"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:48.818870200Z",
  "dados": {
    "agrupamento": "DIA",
    "pontos": [
      {
        "inicio": "2026-11-03",
        "rotulo": "03/11",
        "faturamento": 982.32,
        "pedidos": 8,
        "ticketMedio": 122.79
      },
      {
        "inicio": "2026-11-04",
        "rotulo": "04/11",
        "faturamento": 1378.36,
        "pedidos": 9,
        "ticketMedio": 153.15
      }
    ]
  }
}
```

### GET /vendas/por-horario

Retorna pedidos válidos e faturamento em cada uma das 24 horas do dia.

Parâmetros: filtros comuns. Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/vendas/por-horario?dataInicio=2026-11-03&dataFim=2026-11-09"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:49.118964300Z",
  "dados": [
    {
      "hora": 18,
      "rotulo": "18h",
      "pedidos": 17,
      "faturamento": 2389.24
    },
    {
      "hora": 19,
      "rotulo": "19h",
      "pedidos": 29,
      "faturamento": 4251.76
    }
  ]
}
```

### GET /vendas/por-tipo

Retorna quantidade, faturamento e participação no faturamento por tipo de produto, do maior para o menor.

Parâmetros: filtros comuns. Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/vendas/por-tipo?dataInicio=2026-11-03&dataFim=2026-11-09"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:49.381684600Z",
  "dados": [
    {
      "tipoProduto": "Pizza",
      "quantidade": 272,
      "faturamento": 13835.75,
      "participacao": 82.57
    },
    {
      "tipoProduto": "Lanche",
      "quantidade": 63,
      "faturamento": 1596.42,
      "participacao": 9.53
    },
    {
      "tipoProduto": "Bebida",
      "quantidade": 60,
      "faturamento": 809.0,
      "participacao": 4.83
    },
    {
      "tipoProduto": "Esfiha",
      "quantidade": 72,
      "faturamento": 514.48,
      "participacao": 3.07
    }
  ]
}
```

### GET /pedidos/status

Retorna a quantidade de pedidos por status, os pedidos em andamento e o tempo médio de atendimento em minutos. `tempoMedioAtendimentoMinutos` fica `null` quando não há pedidos finalizados. O status `INDEFINIDO` aparece apenas quando existem pedidos com status desconhecido.

Parâmetros: filtros comuns. Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/pedidos/status?dataInicio=2026-11-03&dataFim=2026-11-09"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:49.610201500Z",
  "dados": {
    "total": 118,
    "emAndamento": 15,
    "tempoMedioAtendimentoMinutos": 66,
    "status": [
      {
        "status": "RECEBIDO",
        "rotulo": "Recebido",
        "quantidade": 4,
        "percentual": 3.39
      },
      {
        "status": "EM_PREPARACAO",
        "rotulo": "Em preparação",
        "quantidade": 4,
        "percentual": 3.39
      }
    ]
  }
}
```

### GET /produtos/mais-vendidos

Retorna o ranking de produtos por quantidade vendida. `participacao` é o percentual da quantidade total.

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| Filtros comuns | | | |
| `limite` | inteiro | `10` | De 1 a 50 |

Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/produtos/mais-vendidos?dataInicio=2026-11-03&dataFim=2026-11-09&limite=3"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:49.899646200Z",
  "dados": [
    {
      "posicao": 1,
      "produtoId": "6af25493493b3e4e6291a72b",
      "produtoNome": "Pizza Calabresa",
      "tipoProduto": "Pizza",
      "quantidade": 87,
      "faturamento": 4273.94,
      "participacao": 18.63
    },
    {
      "posicao": 2,
      "produtoId": "6af25493493b3e4e6291a72e",
      "produtoNome": "Pizza Frango com Catupiry",
      "tipoProduto": "Pizza",
      "quantidade": 73,
      "faturamento": 4045.59,
      "participacao": 15.63
    },
    {
      "posicao": 3,
      "produtoId": "6af25493493b3e4e6291a72d",
      "produtoNome": "Pizza Portuguesa",
      "tipoProduto": "Pizza",
      "quantidade": 57,
      "faturamento": 3082.64,
      "participacao": 12.21
    }
  ]
}
```

### GET /pedidos

Retorna os pedidos do período, do mais recente para o mais antigo. Uma página além do fim retorna lista vazia.

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| Filtros comuns | | | |
| `pagina` | inteiro | `0` | Página, a partir de 0 |
| `tamanho` | inteiro | `20` | De 1 a 100 |

Erros: 400, 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/pedidos?dataInicio=2026-11-03&dataFim=2026-11-09&status=FINALIZADO&pagina=0&tamanho=2"
```

```json
{
  "periodo": {
    "inicio": "2026-11-03",
    "fim": "2026-11-09"
  },
  "atualizadoEm": "2026-11-09T23:19:50.141457900Z",
  "dados": {
    "pagina": 0,
    "tamanho": 2,
    "totalElementos": 93,
    "totalPaginas": 47,
    "itens": [
      {
        "id": "6af25495493b3e4e6291ad1d",
        "recebidoEm": "2026-11-09T21:49:34Z",
        "status": "FINALIZADO",
        "statusRotulo": "Finalizado",
        "produtos": "2x Pizza Calabresa",
        "quantidadeItens": 2,
        "percentualDesconto": 0,
        "valor": 99.8
      },
      {
        "id": "6af25495493b3e4e6291ad1c",
        "recebidoEm": "2026-11-09T21:26:32Z",
        "status": "FINALIZADO",
        "statusRotulo": "Finalizado",
        "produtos": "3x Pizza Portuguesa, 1x Pizza Calabresa, 1x Esfiha de Carne",
        "quantidadeItens": 5,
        "percentualDesconto": 0,
        "valor": 222.1
      }
    ]
  }
}
```

### GET /filtros

Retorna as opções para montar os filtros e o período padrão. Inclui produtos inativos, porque eles podem constar em pedidos antigos. Esta rota não recebe parâmetros e não usa o formato `periodo`, `atualizadoEm` e `dados`.

Erros: 401, 403, 500 e 503.

```bash
curl -u gerente:SUA_SENHA "http://localhost:8080/api/dashboard/filtros"
```

```json
{
  "status": [
    {
      "valor": "RECEBIDO",
      "rotulo": "Recebido"
    },
    {
      "valor": "EM_PREPARACAO",
      "rotulo": "Em preparação"
    }
  ],
  "tiposProduto": [
    "Bebida",
    "Esfiha",
    "Lanche",
    "Pizza"
  ],
  "produtos": [
    {
      "id": "6af25493493b3e4e6291a72f",
      "nome": "Esfiha de Carne",
      "tipo": "Esfiha"
    },
    {
      "id": "6af25493493b3e4e6291a730",
      "nome": "Esfiha de Queijo",
      "tipo": "Esfiha"
    }
  ],
  "agrupamentos": [
    {
      "valor": "DIA",
      "rotulo": "Por dia"
    },
    {
      "valor": "SEMANA",
      "rotulo": "Por semana"
    },
    {
      "valor": "MES",
      "rotulo": "Por mês"
    }
  ],
  "periodoPadrao": {
    "inicio": "2026-10-11",
    "fim": "2026-11-09"
  }
}
```
