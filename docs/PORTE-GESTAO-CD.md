# Porte do recebimento do Gestão CD

O Gestão CD (Cloudflare Worker + D1, TypeScript) já tem o módulo de recebimento usado no CD. Este projeto reaproveita as **regras, telas e fluxos** dele, reescritos em Java/Spring e Vue, no padrão pedido pelo teste. Não há código copiado: o stack é outro, então cada parte foi reimplementada com testes.

Legenda: ✅ portado · 🟡 portado em parte / adaptado · ⏳ pendente · 🔮 futuro (fora do escopo desta entrega)

## Agendamento

| Rota no Gestão CD | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/recebimento/agendamentos/novo` | Novo agendamento com ocupação por dia e faixa | ✅ | Novo agendamento: % da data e de cada faixa, simulação ao vivo, alerta sem bloqueio |
| `/recebimento/agendamentos/buscar` | Busca por período, fornecedor, pedido e status | ✅ | Agendamentos (filtros + detalhe) |
| `/recebimento/agendamentos/dashboard` | Painel do dia | ✅ | Início: ocupação vs 480, 4 barras vs 120, próximos caminhões, status, alertas, 7 dias |
| `/recebimento/agendamentos/links` | Link público para o fornecedor | ✅ | Link no e-mail + botão "Link do fornecedor" (gera novo e invalida o anterior; token guardado só como hash SHA-256) |
| `/agendar/meu/...` | Página pública do fornecedor | ✅ | `/fornecedor/:token`: ver agendamento, confirmar presença, informar placa e motorista e **remarcar sozinho** numa grade de 14 dias × 4 faixas. O fornecedor só escolhe faixa com vaga (o servidor recusa se estourar); o CD pode remarcar mesmo estourando, com alerta |
| `/recebimento/agendamentos/cadastrar-fornecedor` | Cadastro de fornecedor | ✅ | Cadastros → Fornecedores (CNPJ com dígito verificador, único). No novo agendamento, "Fornecedor cadastrado" preenche nome e e-mail e vincula o agendamento |
| `/recebimento/agendamentos/conferir-produto` | Produtos do pedido dentro do agendamento | ✅ | Itens da NF-e importada no detalhe do agendamento (código, descrição, quantidade, peso) |
| Remarcação pelo CD | Trocar data/faixa | ✅ | Detalhe → "Remarcar" (mesma grade; avisa o fornecedor por push e registra na auditoria) |
| Multi-CD | Mais de um centro de distribuição | ✅ | Cadastros → Centros de distribuição. Cada CD pode ter capacidade própria por dia e por faixa; vazio = 480/120. O CD 1 é o do desafio e não pode ser inativado. Seletor de CD em novo agendamento, lista, divergências, descarga e rotina (aparece só quando há mais de um CD) |
| Capacidade (migração 0159) | 480/dia, faixas, fator não paletizado | ✅ | Cadastros de capacidade, faixas, caminhões e paletização |
| Histórico do agendamento (0125) | Quem fez o quê | ✅ | Auditoria (ADMIN) com usuário, data/hora, ação, entidade, IP e detalhes JSON |

## Recebimento operacional

| Rota | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/recebimento-operacional/portaria` | Chegada do caminhão (placa, motorista, hora) | ✅ | Detalhe do agendamento → "Registrar chegada" (valida placa Mercosul/antiga) |
| `/recebimento-operacional/novo` | Início e fim da conferência | ✅ | "Iniciar conferência" (conferente) e "Concluir recebimento" (paletes recebidos) |
| `/recebimento-operacional/divergencias` | Divergência entre agendado e recebido | ✅ | Divergência automática ao concluir + tela **Divergências** (período, fornecedor, CD) com diferença de paletes, NF-e e peso divergente, e **exportação CSV** |
| Não comparecimento | Marca falta do fornecedor | ✅ | "Não compareceu" (libera a capacidade) |
| `/recebimento-relatorio` | Relatório do período | ✅ | Relatório com totais por dia/faixa e CSV (protegido contra injeção de fórmula) |
| `/recebimento-descarga-relatorio`, `/recebimento-parametros-descarga` | Tempos e custo de descarga | ✅ | Cadastros → Tempos de descarga (previsto, tolerância de espera e custo por palete, por tipo de caminhão) + relatório **Tempos de descarga** (espera, descarga, excesso, custo, resumo por caminhão, CSV) |
| `/recebimento-produtos` (novo, importar-nf, conferir, exportar) | Itens e importação de XML da NF-e | ✅ | "Importar XML" no detalhe (após a chegada): lê `nfeProc`/`NFe` com parser protegido contra XXE e limite de 2 MB, cria os produtos que faltam, calcula o peso pelos pesos cadastrados, compara com o peso da NF (5% de tolerância), vincula o fornecedor pelo CNPJ e avisa itens sem peso. Chave duplicada é recusada |
| `/recebimento/produtos/pesos` | Peso por produto | ✅ | Cadastros → Produtos e pesos (filtro "somente sem peso", peso bruto e itens por palete) |
| `/recebimento-fornecedores` | Lista de fornecedores | ✅ | Cadastros → Fornecedores |
| `/recebimento-whatsapp` | Aviso por WhatsApp | 🔮 | Futuro. Nesta entrega o aviso é por e-mail + Web Push; o WhatsApp entraria como mais um adaptador da porta de notificação |
| `/recebimento/rotina`, `/recebimento-loja` | Rotina diária e visão da loja | ✅ | **Rotina do dia**: amanhã sem confirmação, atrasados (+30 min), espera acima da tolerância, descarga longa, recebidos sem NF-e, divergências, vales pendentes e cobranças vencidas, cada item com link. **Paletes nas lojas**: saldo por loja |

## Vale-palete

| Rota | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/vale-palete/novo` | Emitir vale (PBR, CHEP, Chapatex, descartáveis) | ✅ | Novo vale, já preenchido a partir do agendamento |
| Assinaturas (0095/0096/0142) | Motorista e conferente assinam na tela | ✅ | Quadro de assinatura (dedo/mouse), refazer assinatura; status Assinado com as duas |
| Impressão | Vale impresso A4 | ✅ | Botão Imprimir (CSS de impressão, sem menu) |
| Devolução / saldo | Baixa de paletes devolvidos | ✅ | Registrar devolução; quita sozinho quando o saldo zera |
| Busca por código | Leitura do código VP | ✅ | Campo de código na lista (`VP-AAAAMMDD-NNNN`) |
| `/vale-palete/conta-corrente` | Saldo por fornecedor | ✅ | Paletes → Conta-corrente: entregues − devolvidos − cobrados, valor a receber |
| `/vale-palete/configuracoes` | Tipos de palete configuráveis | ⏳ | Tipos fixos (PBR, CHEP, Chapatex, descartáveis), iguais aos usados no CD |
| `/paletes/cobranca`, `/pagar`, `/recibos`, `/email` | Cobrança de paletes e recibos | ✅ | Cobrar (limitado ao saldo), receber (PIX, boleto, dinheiro, desconto na NF, devolução física) com **recibo impresso** `RP-AAAAMMDD-NNNN` e logo, cancelar com motivo. O e-mail da cobrança ficou de fora (🔮) |
| `/paletes/retorno`, `/romaneios`, `/saldo-inicial`, `/movimentos`, `/rede` | Movimento entre lojas | ✅ | Paletes → Paletes nas lojas: romaneio `RM-AAAAMMDD-NNNN` de envio, retorno ou saldo inicial, por tipo de palete, e saldo por loja |

## Visual

Padrão do Gestão CD aplicado: telas densas e em pt-BR, campos compactos cinza-claro, botões verdes para confirmar, vermelho claro para excluir/cancelar, toasts, trava de duplo clique, tudo por variáveis CSS e sem `!important`. O logo da Rede Krill (o mesmo arquivo do Gestão CD) aparece no login, no topo, no ícone do PWA/favicon, no e-mail ao fornecedor, na página pública, no vale-palete e no recibo impressos, sempre com a proporção original.

## Resumo

- **Portado:** agendamento completo, dashboard, link público com remarcação pelo fornecedor, remarcação pelo CD, multi-CD, cadastros de fornecedores, produtos/pesos e tempos de descarga, importação do XML da NF-e, portaria e conferência, divergências com CSV, relatório de descarga, rotina do dia, não comparecimento, relatório/CSV, auditoria, vale-palete com assinatura/impressão/devolução, conta-corrente, cobrança e recibo de paletes, romaneios e saldo por loja.
- **Pendente / futuro:** WhatsApp (🔮), e-mail de cobrança de paletes (🔮), tipos de palete configuráveis (⏳).
