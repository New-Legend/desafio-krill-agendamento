# Porte do recebimento do Gestão CD

O Gestão CD (Cloudflare Worker + D1, TypeScript) é o sistema de origem: já tem o módulo de recebimento usado no CD. O **Agendamento Rede Krill** (este projeto) reaproveita as **regras, telas e fluxos** dele, reescritos em Java/Spring e Vue, no padrão pedido pelo teste. Não há código copiado: o stack é outro, então cada parte foi reimplementada com testes.

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
| Histórico do agendamento (0125) | Quem fez o quê | ✅ | Auditoria (permissão 1005) com usuário, data/hora, ação, entidade, IP e detalhes JSON |

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
| `/recebimento-whatsapp` | Aviso por WhatsApp | ✅ | Canal plugável (porta `NotificadorWhatsApp`). Padrão: botão **WhatsApp** no detalhe do agendamento e na cobrança gera link `wa.me` com a mensagem pronta (o usuário confirma o envio no próprio WhatsApp) e registra o uso. Adaptador da API do WhatsApp Business (Cloud API) pronto e **desligado**; liga com `WHATSAPP_PROVEDOR=cloud-api` + token. Nenhum envio externo automático por padrão |
| `/recebimento-conferentes` | Conferentes por CD | ✅ | Cadastros → Conferentes (nome completo, único por CD, ativo/pausado). Ao iniciar a conferência, a tela mostra a lista do CD; se o CD tem conferentes ativos, o servidor só aceita um deles (sem cadastro continua texto livre, como no desafio) |
| `/yms/docas`, `/yms/quadro` (pátio) | Docas por CD, quadro do pátio e alocação de doca | ✅ | Cadastros → **Docas** (código único por CD, ativa/inativa) e Recebimento → **Pátio e docas**: colunas Previstos → Aguardando doca → Na doca → Descarregando → Liberados, faixa de docas livres/ocupadas, atribuir/liberar doca (só caminhão que chegou; a doca precisa ser do mesmo CD, ativa e livre) e indicador **"CD lotado agora"** (todas as docas ocupadas) e **"capacidade do dia"** (paletes/caminhões do dia frente à capacidade). Atualiza a cada 30 s. Só a parte de recebimento: a expedição (carregamento) do YMS não entra neste sistema |
| Capacidade de recebimento por dia (pedido do dono) | Capacidade por CD e por loja | ✅ | Cadastros → **Capacidade por CD e loja**: limite diário em paletes e/ou caminhões para cada CD ou loja (entrega direta), geral ou por dia da semana (a regra do dia vale sobre a geral). CD sem regra continua usando a capacidade do cadastro de CDs (480 no CD 1). O novo agendamento mostra o % do destino e, se passar, devolve alerta (não bloqueia, mesma regra do desafio). Consulta "Está cheio?" por data, CD ou loja. Reaproveita o modelo de capacidade/faixas existente: as faixas continuam valendo para o CD |
| Recebimento por e-mail (`recebimento-email.ts`) | Ler XML de NF-e que chega numa caixa de e-mail | 🔮 | Exige caixa de entrada própria; nesta entrega o XML é importado manualmente no detalhe |
| Cobrança de descarga por modal (`recebimento-cobranca.ts`) | Taxa de descarga sider/baú/granel por palete | ✅ / 🟡 | Cadastros → **Taxa de descarga**: valor por palete por modal, com as tarifas padrão do Gestão CD (sider/carreta R$ 25, baú R$ 30, granel R$ 60) e modais novos. O modal vem do agendamento ou é deduzido do tipo de caminhão como no Gestão CD (granel/rebater → granel, baú → baú, demais → sider). A taxa (paletes × valor) aparece em cada caminhão do pátio. Não portado: a divisão palete fechado × gerado na mesma carga (modal "misto") |
| `/recebimento-produtos/exportar`, `novo` | Exportar catálogo e incluir produto avulso | ✅ | Produtos e pesos → **Exportar catálogo (CSV)** (separador `;`, BOM para Excel, proteção contra fórmula). No detalhe do agendamento (depois da chegada) → **Produtos avulsos**: item que chegou fora da NF-e, localizado pelo código/EAN no catálogo ou cadastrado na hora. `exportar-excel` (xlsx) não foi portado; o CSV abre no Excel |
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
| `/vale-palete/configuracoes` | Tipos de palete configuráveis | ✅ / 🟡 | Cadastros → **Tipos de palete** (permissão 1410): código (imutável), nome, valor de reposição, ordem e situação. Semente: PBR, CHEP, Chapatex, Descartável. Vales e romaneios guardam `{CÓDIGO: quantidade}` (JSONB, migração V5 converte os dados antigos); novos só aceitam tipos ativos; tipo usado não pode ser excluído, só inativado; sempre fica ao menos um ativo. O valor de reposição sugere o preço na cobrança. Também da mesma tela: tipo **por CD** (vazio = todos os CDs), **exigir as duas assinaturas** (ligado, a devolução só é registrada depois de motorista e conferente assinarem; desligado por padrão) e **agrupar a conta-corrente por fornecedor ou transportadora** (aba "Por transportadora", usando a transportadora do agendamento; sem ela entra o fornecedor). As cobranças continuam por fornecedor |
| `/paletes/cobranca`, `/pagar`, `/recibos`, `/email` | Cobrança de paletes e recibos | ✅ | Cobrar (limitado ao saldo), receber (PIX, boleto, dinheiro, desconto na NF, devolução física) com **recibo impresso** `RP-AAAAMMDD-NNNN` e logo, cancelar com motivo. **E-mail** da cobrança (ou do recibo, se paga) pelo SMTP do sistema (Mailpit no Docker), com template da Rede Krill, pré-visualização, destinatário sugerido (cadastro do fornecedor ou último agendamento) e **histórico de envios** (`notificacao_envio` + auditoria). Diferença: o Gestão CD anexa PDF e também envia o vale; aqui vai o HTML da cobrança/recibo |
| `/paletes/retorno`, `/romaneios`, `/saldo-inicial`, `/movimentos`, `/rede` | Movimento entre lojas | ✅ | Paletes → Paletes nas lojas: romaneio `RM-AAAAMMDD-NNNN` de envio, retorno ou saldo inicial, por tipo de palete, e saldo por loja |

## Acessos

| Gestão CD | O que faz | Status | Como ficou aqui |
|---|---|---|---|
| `permissoes-numeros.ts` | Número fixo por permissão, por módulo | ✅ | `Permissao.java`: 1001–1005 Administração, 1101–1106 Operação, 1201–1207 Recebimento, 1301–1308 Paletes, 1401–1411 Cadastros |
| `/usuarios/` | Lista, pílulas, último acesso, menu ⋮, modal | ✅ | Administração → **Usuários** (1001/1002): filtros por nome, perfil e situação; cartões no celular; senha provisória com troca obrigatória |
| Perfis de acesso | Perfil = conjunto de permissões | ✅ | **Perfis e permissões** (1003), marcando por módulo. Semente: Administrador, Recebimento, Portaria, Consulta |
| Botão de permissões da tela | Mostra os números usados | ✅ | Botão **Permissões** em cada tela, com liberada/sem acesso e atalho para o editor |
| `logs-acesso.ts` | Entradas, saídas e senhas recusadas | ✅ | **Logs de acesso** (1004) com período, evento, IP e navegador resumido |
| Push por permissão (`queuePushNotificationsForCd`) | Aviso só para quem tem a permissão | 🟡 | Filtra por 1102. Sem filtro por CD, sem fila/deduplicação e sem o resumo "Fornecedores não compareceram" |
| Níveis acima do administrador | Acesso de suporte | ❌ | Não portado: o código é público |

## Visual

Padrão do Gestão CD aplicado: telas densas e em pt-BR, campos compactos cinza-claro, botões verdes para confirmar, vermelho claro para excluir/cancelar, toasts, trava de duplo clique, tudo por variáveis CSS e sem `!important`. O logo da Rede Krill (o mesmo arquivo do Gestão CD) aparece no login, no topo, no ícone do PWA/favicon, no e-mail ao fornecedor, na página pública, no vale-palete e no recibo impressos, sempre com a proporção original.

## Resumo

- **Portado:** agendamento completo, dashboard, link público com remarcação pelo fornecedor, remarcação pelo CD, multi-CD, cadastros de fornecedores, produtos/pesos e tempos de descarga, importação do XML da NF-e, portaria e conferência, divergências com CSV, relatório de descarga, rotina do dia, não comparecimento, relatório/CSV, auditoria, vale-palete com assinatura/impressão/devolução, conta-corrente, cobrança e recibo de paletes, romaneios e saldo por loja.
- **Portado nesta rodada:** tipos de palete configuráveis, e-mail de cobrança/recibo de paletes com histórico, WhatsApp como canal plugável (link wa.me; API do WhatsApp Business pronta e desligada) e conferentes por CD.
- **Portado na rodada de 29/10/2026:** pátio e docas do recebimento (cadastro de docas, quadro do pátio, atribuição de doca, CD lotado agora e no dia), capacidade diária de recebimento por CD e por loja (paletes/caminhões, por dia da semana, com % no agendamento), taxa de descarga por modal, exportação do catálogo em CSV, produto avulso no recebimento, tipos de palete por CD, "exigir as duas assinaturas" e agrupamento por transportadora.
- **Pendente / futuro:** leitura de NF-e por caixa de e-mail (🔮), PDF anexado e e-mail do vale (🟡), carga mista palete fechado × gerado na taxa de descarga (🟡), expedição/carregamento do YMS (fora do escopo de recebimento).
- **Fora de propósito:** níveis de acesso especiais do Gestão CD, credenciais e dados reais da Krill não foram copiados.
