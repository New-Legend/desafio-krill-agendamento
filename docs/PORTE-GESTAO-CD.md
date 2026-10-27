# Porte do recebimento do Gestão CD

O Gestão CD (Cloudflare Worker + D1, TypeScript) já tem o módulo de recebimento usado no CD. Este projeto reaproveita as **regras, telas e fluxos** dele, reescritos em Java/Spring e Vue, no padrão pedido pelo teste. Não há código copiado: o stack é outro, então cada parte foi reimplementada com testes.

Legenda: ✅ portado · 🟡 portado em parte / adaptado · ⏳ pendente (fora do prazo de 04/11)

## Agendamento

| Rota no Gestão CD | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/recebimento/agendamentos/novo` | Novo agendamento com ocupação por dia e faixa | ✅ | Novo agendamento: % da data e de cada faixa, simulação ao vivo, alerta sem bloqueio |
| `/recebimento/agendamentos/buscar` | Busca por período, fornecedor, pedido e status | ✅ | Agendamentos (filtros + detalhe) |
| `/recebimento/agendamentos/dashboard` | Painel do dia | ✅ | Início: ocupação vs 480, 4 barras vs 120, próximos caminhões, status, alertas, 7 dias |
| `/recebimento/agendamentos/links` | Link público para o fornecedor | ✅ | Link no e-mail + botão "Link do fornecedor" (gera novo e invalida o anterior; token guardado só como hash SHA-256) |
| `/agendar/meu/...` | Página pública do fornecedor | 🟡 | `/fornecedor/:token`: ver agendamento, confirmar presença, informar placa e motorista. O fornecedor ainda não escolhe a data sozinho |
| `/recebimento/agendamentos/cadastrar-fornecedor` | Cadastro de fornecedor | ⏳ | Hoje o fornecedor vem digitado ou do ERP |
| `/recebimento/agendamentos/conferir-produto` | Produtos do pedido dentro do agendamento | ⏳ | — |
| Capacidade (migração 0159) | 480/dia, faixas, fator não paletizado | ✅ | Cadastros de capacidade, faixas, caminhões e paletização |
| Histórico do agendamento (0125) | Quem fez o quê | ✅ | Auditoria (ADMIN) com usuário, data/hora, ação, entidade, IP e detalhes JSON |

## Recebimento operacional

| Rota | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/recebimento-operacional/portaria` | Chegada do caminhão (placa, motorista, hora) | ✅ | Detalhe do agendamento → "Registrar chegada" (valida placa Mercosul/antiga) |
| `/recebimento-operacional/novo` | Início e fim da conferência | ✅ | "Iniciar conferência" (conferente) e "Concluir recebimento" (paletes recebidos) |
| `/recebimento-operacional/divergencias` | Divergência entre agendado e recebido | 🟡 | Divergência automática ao concluir, alerta no Início e push. Falta tela própria com exportação |
| Não comparecimento | Marca falta do fornecedor | ✅ | "Não compareceu" (libera a capacidade) |
| `/recebimento-relatorio` | Relatório do período | ✅ | Relatório com totais por dia/faixa e CSV (protegido contra injeção de fórmula) |
| `/recebimento-descarga-relatorio`, `/recebimento-parametros-descarga` | Tempos e custo de descarga | ⏳ | Os horários de chegada/início/fim já são gravados, falta o relatório |
| `/recebimento-produtos` (novo, importar-nf, conferir, exportar) | Itens e importação de XML da NF-e | ⏳ | — |
| `/recebimento/produtos/pesos` | Peso por produto | ⏳ | — |
| `/recebimento-fornecedores` | Lista de fornecedores | ⏳ | — |
| `/recebimento-whatsapp` | Aviso por WhatsApp | ⏳ | Substituído por e-mail + Web Push |
| `/recebimento/rotina`, `/recebimento-loja` | Rotina diária e visão da loja | ⏳ | — |

## Vale-palete

| Rota | O que faz | Status | Onde ficou aqui |
|---|---|---|---|
| `/vale-palete/novo` | Emitir vale (PBR, CHEP, Chapatex, descartáveis) | ✅ | Novo vale, já preenchido a partir do agendamento |
| Assinaturas (0095/0096/0142) | Motorista e conferente assinam na tela | ✅ | Quadro de assinatura (dedo/mouse), refazer assinatura; status Assinado com as duas |
| Impressão | Vale impresso A4 | ✅ | Botão Imprimir (CSS de impressão, sem menu) |
| Devolução / saldo | Baixa de paletes devolvidos | ✅ | Registrar devolução; quita sozinho quando o saldo zera |
| Busca por código | Leitura do código VP | ✅ | Campo de código na lista (`VP-AAAAMMDD-NNNN`) |
| `/vale-palete/conta-corrente` | Saldo por fornecedor | ⏳ | — |
| `/vale-palete/configuracoes` | Tipos de palete configuráveis | ⏳ | Tipos fixos nesta versão |
| `/paletes/cobranca`, `/pagar`, `/recibos`, `/email` | Cobrança de paletes e recibos | ⏳ | — |
| `/paletes/retorno`, `/romaneios`, `/saldo-inicial`, `/movimentos`, `/rede` | Movimento entre lojas | ⏳ | — |

## Visual

Padrão do Gestão CD aplicado: telas densas e em pt-BR, campos compactos cinza-claro, botões verdes para confirmar, vermelho claro para excluir/cancelar, toasts, trava de duplo clique, tudo por variáveis CSS e sem `!important`.

## Resumo

- **Portado:** agendamento completo, dashboard, link público do fornecedor, portaria e conferência, divergência automática, não comparecimento, relatório/CSV, auditoria, vale-palete com assinatura, impressão e devolução.
- **Pendente:** produtos e NF-e, fornecedores, tempos de descarga, cobrança e conta-corrente de paletes, WhatsApp, rotina/loja, multi-CD.
