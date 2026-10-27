# Integração com o Bluesoft ERP

## Objetivo

Quando o usuário digita o **ID do pedido** no novo agendamento, o sistema busca o pedido de compra no ERP e preenche sozinho o fornecedor, o e-mail, a quantidade de paletes e a paletização. O usuário confere e salva.

## Como está montado

```
NovoAgendamento (tela) ── GET /api/v1/erp/pedidos/{id}
        └── PedidoCompraUseCase (application)
                └── PedidoCompraGateway (porta)
                        ├── PedidoCompraLocalGateway     (padrão, ERP_PROVEDOR=local)
                        └── PedidoCompraBluesoftGateway  (ERP_PROVEDOR=bluesoft)
```

- A regra não sabe qual ERP está ligado. Para trocar de ERP basta criar outro adaptador.
- O adaptador Bluesoft vem **desligado**. Ele **não foi testado contra a API real** porque não temos credenciais. A base local simula as respostas.
- Se o ERP falhar ou não achar o pedido, a tela segue normal e o usuário digita os dados.

## Configuração

| Variável | Exemplo | Uso |
|---|---|---|
| `ERP_PROVEDOR` | `local` ou `bluesoft` | Qual adaptador usar |
| `BLUESOFT_BASE_URL` | `https://erp.bluesoft.com.br` | Endereço do ERP |
| `BLUESOFT_TENANT` | `redekrill` | Nome do ambiente no endereço |
| `BLUESOFT_CLIENT_ID` / `BLUESOFT_CLIENT_SECRET` | — | Credenciais da aplicação cadastrada no Bluesoft |
| `BLUESOFT_TIMEOUT` | `10` (segundos) | Tempo máximo de espera |

## Autenticação

`POST https://erp.bluesoft.com.br/<tenant>/oauth2/token` (form-urlencoded) com `grant_type=client_credentials`, `client_id` e `client_secret`. O token fica em cache até perto de expirar.

Permissão necessária no usuário de integração: **3122 - API Pedidos Efetivados (Consultar)**.

## Mapa dos campos

| Campo do agendamento | De onde vem no Bluesoft | Endpoint | Situação |
|---|---|---|---|
| ID Pedido | filtro `pedidoCompraKeys` | `GET /<tenant>/api/compras/pedidos-de-compra?pedidoCompraKeys={id}&page=0&size=1` | Endpoint confirmado; nome do filtro a confirmar no Redoc |
| Fornecedor | `fornecedor.razaoSocial` (ou `nome`) | mesmo | Nome do campo a confirmar no Redoc |
| CNPJ (só exibição) | `fornecedor.cpfCnpj` (ou `cnpj`) | mesmo | A confirmar no Redoc |
| E-mail do fornecedor | `fornecedor.email` (ou `emailContato`) | mesmo, ou cadastro de fornecedor (`/api/comercial/fornecedores/{key}`) | A confirmar no Redoc |
| Quantidade de paletes | `quantidadePaletes`; se não vier, o usuário digita (evolução: somar itens ÷ lastro×altura do produto) | mesmo | A confirmar no Redoc |
| Tipo de paletização | não vem do pedido; o usuário escolhe (o adaptador local sugere) | — | Pendente |
| Data prevista | `dataPrevistaEntrega` (ou `dataEntrega`) | mesmo | A confirmar no Redoc |
| Loja / CD de destino | `lojaKey` (ou `loja`) | mesmo | A confirmar no Redoc |
| Situação do pedido | `situacao` (ou `status`) | mesmo | A confirmar no Redoc |

O adaptador aceita os nomes alternativos acima, então mudanças pequenas de nome não quebram a tela.

## Próximos passos para ligar em produção

1. Pedir ao TI o cadastro da aplicação OAuth no Bluesoft com a permissão 3122.
2. Abrir o Redoc do tenant e confirmar os nomes da tabela acima.
3. Subir com `ERP_PROVEDOR=bluesoft` em homologação e testar com 3 ou 4 pedidos reais.
