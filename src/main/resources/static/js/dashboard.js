(function () {
    const raiz = document.getElementById('dashboard');
    const API = raiz.dataset.api;
    const URL_LOGIN = raiz.dataset.login;
    const INTERVALO_ATUALIZACAO = 60000;
    const TAMANHO_PAGINA = 10;
    const FUSO = 'America/Sao_Paulo';
    const COR_SERIE = '#2a78d6';
    const COR_AREA = 'rgba(42, 120, 214, 0.10)';
    const COR_GRADE = '#e1e0d9';
    const COR_TEXTO = '#52514e';

    const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const numero = new Intl.NumberFormat('pt-BR');
    const decimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
    const dataHora = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, dateStyle: 'short', timeStyle: 'short' });
    const hora = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, timeStyle: 'medium' });

    const campo = id => document.getElementById(id);
    const estado = { pagina: 0, opcoes: null, requisicoes: {}, temporizador: null };
    const graficos = {};

    function elemento(tag, classes, texto) {
        const novo = document.createElement(tag);
        if (classes) {
            novo.className = classes;
        }
        if (texto !== undefined && texto !== null) {
            novo.textContent = texto;
        }
        return novo;
    }

    function linhaTabela(celulas) {
        const linha = elemento('tr');
        celulas.forEach(celula => linha.appendChild(celula));
        return linha;
    }

    function celula(texto, classes) {
        return elemento('td', classes, texto);
    }

    function substituir(alvo, filhos) {
        alvo.replaceChildren(...filhos);
    }

    function percentual(valor) {
        return decimal.format(valor) + '%';
    }

    function filtrosAtuais() {
        const filtros = {
            dataInicio: campo('filtro-inicio').value,
            dataFim: campo('filtro-fim').value,
            status: campo('filtro-status').value,
            tipoProduto: campo('filtro-tipo').value,
            produtoId: campo('filtro-produto').value
        };
        Object.keys(filtros).forEach(chave => {
            if (!filtros[chave]) {
                delete filtros[chave];
            }
        });
        return filtros;
    }

    async function consultar(caminho, parametros) {
        const url = API + caminho + '?' + new URLSearchParams(parametros || {}).toString();
        let resposta;
        try {
            resposta = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
        } catch (erro) {
            throw new Error('Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.');
        }
        if (resposta.status === 401) {
            window.location.href = URL_LOGIN;
            throw new Error('Sua sessão expirou.');
        }
        const corpo = await resposta.json().catch(() => null);
        if (!resposta.ok) {
            throw new Error(corpo && corpo.mensagem ? corpo.mensagem : 'Não foi possível carregar os dados do dashboard.');
        }
        return corpo;
    }

    function novaRequisicao(nome) {
        estado.requisicoes[nome] = (estado.requisicoes[nome] || 0) + 1;
        return estado.requisicoes[nome];
    }

    function requisicaoAtual(nome, numeroRequisicao) {
        return estado.requisicoes[nome] === numeroRequisicao;
    }

    function mostrarErro(mensagem) {
        const alerta = campo('dashboard-erro');
        alerta.textContent = mensagem;
        alerta.classList.remove('d-none');
    }

    function limparErro() {
        campo('dashboard-erro').classList.add('d-none');
    }

    function indicador(rotulo, valor, detalhe) {
        const coluna = elemento('div', 'col');
        const cartao = elemento('div', 'card shadow-sm h-100');
        const corpo = elemento('div', 'card-body p-3');
        corpo.appendChild(elemento('div', 'dashboard-indicador-rotulo', rotulo));
        corpo.appendChild(elemento('div', 'dashboard-indicador-valor', valor));
        if (detalhe) {
            corpo.appendChild(detalhe);
        }
        cartao.appendChild(corpo);
        coluna.appendChild(cartao);
        return coluna;
    }

    function detalheTexto(texto) {
        return elemento('div', 'dashboard-indicador-detalhe', texto);
    }

    function detalheVariacao(variacao) {
        if (variacao === null || variacao === undefined) {
            return detalheTexto('Sem base de comparação');
        }
        const alta = variacao >= 0;
        const detalhe = elemento('div', 'dashboard-indicador-detalhe');
        const destaque = elemento('span', alta ? 'dashboard-delta-alta' : 'dashboard-delta-queda');
        destaque.appendChild(elemento('i', alta ? 'bi bi-arrow-up' : 'bi bi-arrow-down'));
        destaque.appendChild(document.createTextNode(' ' + percentual(Math.abs(variacao))));
        detalhe.appendChild(destaque);
        detalhe.appendChild(document.createTextNode(' vs. período anterior'));
        return detalhe;
    }

    function renderizarIndicadores(resumo, situacao) {
        const tempo = situacao.tempoMedioAtendimentoMinutos;
        substituir(campo('dashboard-indicadores'), [
            indicador('Faturamento', moeda.format(resumo.faturamento), detalheVariacao(resumo.comparativo.variacaoFaturamento)),
            indicador('Pedidos válidos', numero.format(resumo.pedidosValidos), detalheVariacao(resumo.comparativo.variacaoPedidos)),
            indicador('Ticket médio', moeda.format(resumo.ticketMedio), detalheTexto('Descontos: ' + moeda.format(resumo.totalDescontos))),
            indicador('Itens vendidos', numero.format(resumo.itensVendidos), detalheTexto('De ' + numero.format(resumo.totalPedidos) + ' pedidos no total')),
            indicador('Cancelamentos', percentual(resumo.taxaCancelamento), detalheTexto(numero.format(resumo.pedidosCancelados) + ' pedidos cancelados')),
            indicador('Em andamento', numero.format(situacao.emAndamento),
                detalheTexto(tempo === null ? 'Sem pedidos finalizados' : 'Atendimento médio: ' + numero.format(tempo) + ' min'))
        ]);

        const qualidade = resumo.qualidadeDados;
        const avisos = [];
        if (qualidade.pedidosSemData > 0) {
            avisos.push(numero.format(qualidade.pedidosSemData) + ' pedido(s) sem data de recebimento ficaram fora das consultas');
        }
        if (qualidade.pedidosStatusIndefinido > 0) {
            avisos.push(numero.format(qualidade.pedidosStatusIndefinido) + ' pedido(s) com status desconhecido não entraram no faturamento');
        }
        if (qualidade.pedidosSemValor > 0) {
            avisos.push(numero.format(qualidade.pedidosSemValor) + ' pedido(s) sem valor registrado');
        }
        const alerta = campo('dashboard-qualidade');
        alerta.textContent = avisos.length ? 'Atenção aos dados: ' + avisos.join('; ') + '.' : '';
        alerta.classList.toggle('d-none', avisos.length === 0);
    }

    function opcoesEixos(formatarValor) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 0 },
            legend: { display: false },
            hover: { mode: 'index', intersect: false },
            tooltips: { mode: 'index', intersect: false, displayColors: false },
            scales: {
                xAxes: [{
                    gridLines: { display: false, color: COR_GRADE },
                    ticks: { fontColor: COR_TEXTO, autoSkip: true, maxTicksLimit: 12, maxRotation: 0 }
                }],
                yAxes: [{
                    gridLines: { color: COR_GRADE, drawBorder: false, zeroLineColor: COR_GRADE },
                    ticks: { fontColor: COR_TEXTO, beginAtZero: true, maxTicksLimit: 6, callback: formatarValor, precision: 0 }
                }]
            }
        };
    }

    function moedaCompacta(valor) {
        return valor >= 1000 ? 'R$ ' + decimal.format(valor / 1000) + ' mil' : 'R$ ' + numero.format(valor);
    }

    function criarGraficoVendas() {
        const opcoes = opcoesEixos(moedaCompacta);
        opcoes.tooltips.callbacks = {
            label: item => 'Faturamento: ' + moeda.format(item.yLabel),
            afterLabel: item => {
                const ponto = estado.pontosVendas[item.index];
                return ['Pedidos: ' + numero.format(ponto.pedidos), 'Ticket médio: ' + moeda.format(ponto.ticketMedio)];
            }
        };
        return new Chart(campo('grafico-vendas'), {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    data: [],
                    borderColor: COR_SERIE,
                    backgroundColor: COR_AREA,
                    borderWidth: 2,
                    lineTension: 0,
                    pointRadius: 0,
                    pointHitRadius: 12,
                    pointHoverRadius: 4,
                    pointHoverBackgroundColor: COR_SERIE
                }]
            },
            options: opcoes
        });
    }

    function criarGraficoHorario() {
        const opcoes = opcoesEixos(valor => numero.format(valor));
        opcoes.scales.xAxes[0].ticks.maxTicksLimit = 24;
        opcoes.tooltips.callbacks = {
            label: item => 'Pedidos: ' + numero.format(item.yLabel),
            afterLabel: item => 'Faturamento: ' + moeda.format(estado.pontosHorario[item.index].faturamento)
        };
        return new Chart(campo('grafico-horario'), {
            type: 'bar',
            data: {
                labels: [],
                datasets: [{ data: [], backgroundColor: COR_SERIE, hoverBackgroundColor: '#1c5cab', barPercentage: 0.7, categoryPercentage: 0.9 }]
            },
            options: opcoes
        });
    }

    function alternarVazio(idCanvas, idVazio, vazio) {
        campo(idCanvas).parentElement.classList.toggle('d-none', vazio);
        campo(idVazio).classList.toggle('d-none', !vazio);
    }

    function renderizarVendas(serie) {
        estado.pontosVendas = serie.pontos;
        const semVendas = serie.pontos.every(ponto => ponto.pedidos === 0);
        alternarVazio('grafico-vendas', 'vendas-vazio', semVendas);
        graficos.vendas.data.labels = serie.pontos.map(ponto => ponto.rotulo);
        graficos.vendas.data.datasets[0].data = serie.pontos.map(ponto => ponto.faturamento);
        graficos.vendas.update();
        substituir(campo('vendas-tabela'), serie.pontos.map(ponto => linhaTabela([
            celula(ponto.rotulo),
            celula(moeda.format(ponto.faturamento), 'text-end'),
            celula(numero.format(ponto.pedidos), 'text-end'),
            celula(moeda.format(ponto.ticketMedio), 'text-end')
        ])));
    }

    function renderizarHorario(horas) {
        const comPedidos = horas.filter(item => item.pedidos > 0);
        alternarVazio('grafico-horario', 'horario-vazio', comPedidos.length === 0);
        const primeira = comPedidos.length ? comPedidos[0].hora : 0;
        const ultima = comPedidos.length ? comPedidos[comPedidos.length - 1].hora : 23;
        const visiveis = horas.slice(primeira, ultima + 1);
        estado.pontosHorario = visiveis;
        graficos.horario.data.labels = visiveis.map(item => item.rotulo);
        graficos.horario.data.datasets[0].data = visiveis.map(item => item.pedidos);
        graficos.horario.update();
        substituir(campo('horario-tabela'), visiveis.map(item => linhaTabela([
            celula(item.rotulo),
            celula(numero.format(item.pedidos), 'text-end'),
            celula(moeda.format(item.faturamento), 'text-end')
        ])));
    }

    function barra(larguraPercentual) {
        const trilha = elemento('div', 'dashboard-barra-trilha');
        if (larguraPercentual > 0) {
            const preenchimento = elemento('div', 'dashboard-barra-preenchimento');
            preenchimento.style.width = Math.min(100, larguraPercentual) + '%';
            trilha.appendChild(preenchimento);
        }
        return trilha;
    }

    function linhaBarra(rotulo, valor, larguraPercentual) {
        const linha = elemento('div', 'dashboard-barra-linha');
        const texto = elemento('div', 'dashboard-barra-texto');
        texto.appendChild(elemento('span', '', rotulo));
        texto.appendChild(elemento('span', 'dashboard-numeros text-nowrap', valor));
        linha.appendChild(texto);
        linha.appendChild(barra(larguraPercentual));
        return linha;
    }

    function mensagemVazia(texto) {
        return elemento('div', 'dashboard-vazio', texto);
    }

    function renderizarStatus(situacao) {
        if (situacao.total === 0) {
            substituir(campo('pedidos-status'), [mensagemVazia('Nenhum pedido encontrado.')]);
            return;
        }
        substituir(campo('pedidos-status'), situacao.status.map(item => linhaBarra(
            item.rotulo,
            numero.format(item.quantidade) + ' (' + percentual(item.percentual) + ')',
            item.quantidade === 0 ? 0 : item.percentual)));
    }

    function renderizarTipos(tipos) {
        if (tipos.length === 0) {
            substituir(campo('vendas-tipo'), [mensagemVazia('Nenhuma venda encontrada.')]);
            return;
        }
        substituir(campo('vendas-tipo'), tipos.map(item => linhaBarra(
            item.tipoProduto,
            moeda.format(item.faturamento) + ' (' + percentual(item.participacao) + ')',
            item.participacao)));
    }

    function linhaVaziaTabela(colunas, texto) {
        const coluna = celula(texto, 'text-center text-secondary py-4');
        coluna.colSpan = colunas;
        return linhaTabela([coluna]);
    }

    function renderizarRanking(produtos) {
        if (produtos.length === 0) {
            substituir(campo('produtos-ranking'), [linhaVaziaTabela(6, 'Nenhum produto vendido para os filtros selecionados.')]);
            return;
        }
        substituir(campo('produtos-ranking'), produtos.map(produto => {
            const participacao = celula();
            const texto = elemento('div', 'small dashboard-numeros', percentual(produto.participacao));
            participacao.appendChild(texto);
            participacao.appendChild(barra(produto.participacao));
            return linhaTabela([
                celula(produto.posicao, 'dashboard-numeros'),
                celula(produto.produtoNome),
                celula(produto.tipoProduto),
                celula(numero.format(produto.quantidade), 'text-end dashboard-numeros'),
                celula(moeda.format(produto.faturamento), 'text-end dashboard-numeros'),
                participacao
            ]);
        }));
    }

    function etiquetaStatus(pedido) {
        const coluna = celula();
        const classes = pedido.status === 'CANCELADO'
            ? 'badge bg-white text-danger border border-danger'
            : 'badge bg-white text-dark border';
        coluna.appendChild(elemento('span', classes, pedido.statusRotulo));
        return coluna;
    }

    function renderizarPedidos(lista) {
        campo('pedidos-total').textContent = numero.format(lista.totalElementos) + ' pedido(s)';
        const totalPaginas = Math.max(lista.totalPaginas, 1);
        campo('pedidos-pagina').textContent = 'Página ' + (lista.pagina + 1) + ' de ' + totalPaginas;
        campo('pedidos-anterior').disabled = lista.pagina === 0;
        campo('pedidos-proxima').disabled = lista.pagina + 1 >= lista.totalPaginas;
        if (lista.itens.length === 0) {
            substituir(campo('pedidos-lista'), [linhaVaziaTabela(6, 'Nenhum pedido encontrado para os filtros selecionados.')]);
            return;
        }
        substituir(campo('pedidos-lista'), lista.itens.map(pedido => linhaTabela([
            celula(dataHora.format(new Date(pedido.recebidoEm)), 'text-nowrap dashboard-numeros'),
            celula('#' + pedido.id.slice(-6), 'dashboard-numeros'),
            celula(pedido.produtos),
            etiquetaStatus(pedido),
            celula(pedido.percentualDesconto > 0 ? percentual(pedido.percentualDesconto) : '-', 'text-end dashboard-numeros'),
            celula(moeda.format(pedido.valor), 'text-end text-nowrap dashboard-numeros')
        ])));
    }

    function marcarAtualizacao(atualizadoEm) {
        campo('dashboard-atualizado').textContent = 'Atualizado às ' + hora.format(new Date(atualizadoEm));
    }

    function consultaVendas(filtros) {
        return consultar('/vendas', { ...filtros, agrupamento: campo('vendas-agrupamento').value });
    }

    function consultaPedidos(filtros) {
        return consultar('/pedidos', { ...filtros, pagina: estado.pagina, tamanho: TAMANHO_PAGINA });
    }

    async function carregarTudo() {
        const atual = novaRequisicao('tudo');
        novaRequisicao('vendas');
        novaRequisicao('pedidos');
        const filtros = filtrosAtuais();
        raiz.classList.add('atualizando');
        try {
            const [resumo, vendas, horario, tipos, situacao, ranking, pedidos] = await Promise.all([
                consultar('/resumo', filtros),
                consultaVendas(filtros),
                consultar('/vendas/por-horario', filtros),
                consultar('/vendas/por-tipo', filtros),
                consultar('/pedidos/status', filtros),
                consultar('/produtos/mais-vendidos', { ...filtros, limite: 10 }),
                consultaPedidos(filtros)
            ]);
            if (!requisicaoAtual('tudo', atual)) {
                return;
            }
            limparErro();
            renderizarIndicadores(resumo.dados, situacao.dados);
            renderizarVendas(vendas.dados);
            renderizarHorario(horario.dados);
            renderizarTipos(tipos.dados);
            renderizarStatus(situacao.dados);
            renderizarRanking(ranking.dados);
            renderizarPedidos(pedidos.dados);
            marcarAtualizacao(resumo.atualizadoEm);
        } catch (erro) {
            if (requisicaoAtual('tudo', atual)) {
                mostrarErro(erro.message);
            }
        } finally {
            if (requisicaoAtual('tudo', atual)) {
                raiz.classList.remove('atualizando');
            }
        }
    }

    async function carregarParte(nome, consulta, renderizar) {
        const atual = novaRequisicao(nome);
        try {
            const resposta = await consulta(filtrosAtuais());
            if (requisicaoAtual(nome, atual)) {
                limparErro();
                renderizar(resposta.dados);
            }
        } catch (erro) {
            if (requisicaoAtual(nome, atual)) {
                mostrarErro(erro.message);
            }
        }
    }

    function preencherSelect(select, opcoes) {
        const primeira = select.options[0];
        substituir(select, [primeira, ...opcoes.map(opcao => {
            const item = elemento('option', '', opcao.rotulo);
            item.value = opcao.valor;
            return item;
        })]);
    }

    function atualizarProdutos() {
        const tipo = campo('filtro-tipo').value;
        const selecionado = campo('filtro-produto').value;
        const produtos = estado.opcoes.produtos.filter(produto => !tipo || produto.tipo === tipo);
        preencherSelect(campo('filtro-produto'), produtos.map(produto => ({ valor: produto.id, rotulo: produto.nome })));
        campo('filtro-produto').value = produtos.some(produto => produto.id === selecionado) ? selecionado : '';
    }

    function definirPeriodoPadrao() {
        campo('filtro-inicio').value = estado.opcoes.periodoPadrao.inicio;
        campo('filtro-fim').value = estado.opcoes.periodoPadrao.fim;
    }

    function subtrairDias(data, dias) {
        const resultado = new Date(data + 'T12:00:00Z');
        resultado.setUTCDate(resultado.getUTCDate() - dias);
        return resultado.toISOString().slice(0, 10);
    }

    function aplicar() {
        estado.pagina = 0;
        carregarTudo();
    }

    function agendarAtualizacao() {
        clearInterval(estado.temporizador);
        if (campo('dashboard-auto').checked) {
            estado.temporizador = setInterval(() => {
                if (!document.hidden) {
                    carregarTudo();
                }
            }, INTERVALO_ATUALIZACAO);
        }
    }

    function registrarEventos() {
        campo('dashboard-filtros').addEventListener('submit', evento => {
            evento.preventDefault();
            aplicar();
        });
        campo('filtro-limpar').addEventListener('click', () => {
            campo('filtro-status').value = '';
            campo('filtro-tipo').value = '';
            atualizarProdutos();
            campo('filtro-produto').value = '';
            definirPeriodoPadrao();
            aplicar();
        });
        campo('filtro-tipo').addEventListener('change', atualizarProdutos);
        raiz.querySelectorAll('[data-dias]').forEach(botao => botao.addEventListener('click', () => {
            const hoje = estado.opcoes.periodoPadrao.fim;
            campo('filtro-fim').value = hoje;
            campo('filtro-inicio').value = subtrairDias(hoje, Number(botao.dataset.dias) - 1);
            aplicar();
        }));
        campo('dashboard-atualizar').addEventListener('click', carregarTudo);
        campo('dashboard-auto').addEventListener('change', agendarAtualizacao);
        campo('vendas-agrupamento').addEventListener('change', () => carregarParte('vendas', consultaVendas, renderizarVendas));
        campo('pedidos-anterior').addEventListener('click', () => {
            estado.pagina -= 1;
            carregarParte('pedidos', consultaPedidos, renderizarPedidos);
        });
        campo('pedidos-proxima').addEventListener('click', () => {
            estado.pagina += 1;
            carregarParte('pedidos', consultaPedidos, renderizarPedidos);
        });
    }

    async function iniciar() {
        graficos.vendas = criarGraficoVendas();
        graficos.horario = criarGraficoHorario();
        registrarEventos();
        try {
            estado.opcoes = await consultar('/filtros');
        } catch (erro) {
            mostrarErro(erro.message);
            return;
        }
        preencherSelect(campo('filtro-status'), estado.opcoes.status);
        preencherSelect(campo('filtro-tipo'), estado.opcoes.tiposProduto.map(tipo => ({ valor: tipo, rotulo: tipo })));
        atualizarProdutos();
        substituir(campo('vendas-agrupamento'), estado.opcoes.agrupamentos.map(opcao => {
            const item = elemento('option', '', opcao.rotulo);
            item.value = opcao.valor;
            return item;
        }));
        definirPeriodoPadrao();
        await carregarTudo();
        agendarAtualizacao();
    }

    iniciar();
})();
