# Roteiro do vídeo (12 min) e perguntas da entrevista

## Roteiro do vídeo

| Tempo | O que mostrar | O que falar |
|---|---|---|
| 0:00–1:00 | Tela de início | "Este é o sistema de agendamento do recebimento. Ele controla quantos paletes chegam por dia e por faixa, avisa quando passa do limite e acompanha o caminhão até o fim da conferência." |
| 1:00–2:00 | Terminal com `docker compose up --build` | "Um comando sobe tudo: banco, API, tela e uma caixa de e-mail de teste." |
| 2:00–3:30 | Login como admin, Início | Ocupação do dia contra 480, as 4 faixas contra 120 (a vermelha estourou), próximos caminhões, status e alertas. |
| 3:30–5:30 | Novo agendamento | Digitar o pedido 450002: os dados vêm do ERP. Trocar para não paletizado: a ocupação dobra na hora. Escolher uma faixa cheia: aparece o alerta, mas salva. |
| 5:30–6:30 | Mailpit | E-mail que chegou ao fornecedor (com o logo), com o link. Abrir o link: confirmar presença, informar a placa e remarcar para uma faixa com vaga (as cheias ficam bloqueadas). |
| 6:30–8:00 | Detalhe do agendamento | Registrar chegada, iniciar conferência, concluir com 1 palete a menos: a divergência é registrada sozinha. Emitir o vale-palete, assinar com o mouse e imprimir. |
| 8:00–8:45 | Relatório, divergências e rotina | Relatório + CSV; Divergências com a NF-e importada (peso calculado × nota); Rotina do dia com o que falta resolver. |
| 8:45–9:15 | Paletes | Conta-corrente do fornecedor, cobrar, receber e imprimir o recibo; romaneio para a loja. |
| 9:15–9:45 | Login como usuario | Só consulta. Mostrar no Swagger que um POST com esse token volta 403: a regra está no backend. |
| 9:45–10:00 | Auditoria | Quem fez, quando, o quê e de qual IP, inclusive os logins. |
| 10:00–11:00 | Código | Pastas domain, application, infrastructure e web. `CalculadoraOcupacao` e o teste dela. |
| 11:00–11:40 | `mvn verify` e `npm run test:coverage` | Backend 69 testes, domain+application 99,6% de linhas. Frontend 53 testes, 95,8% de linhas nas telas críticas. |
| 11:40–12:00 | Celular / PWA | Instalar o app, receber o push de novo agendamento. Encerrar. |

## Perguntas e respostas

1. **Por que alerta em vez de bloquear?** Porque o pedido diz isso, e na prática o CD às vezes aceita um caminhão a mais. O sistema avisa bem alto (tela, push, painel) e deixa a decisão com a pessoa.
2. **Como o não paletizado conta em dobro?** Cada tipo de paletização tem um fator (1 ou 2). A ocupação é a quantidade vezes o fator, e esse número fica gravado no agendamento.
3. **Se eu mudar o fator depois, o histórico muda?** Não. O valor equivalente é gravado no momento do agendamento.
4. **E se dois usuários agendarem ao mesmo tempo?** Uso uma trava do Postgres por dia (`pg_advisory_xact_lock`). Um espera o outro terminar, então a conta e o alerta saem certos.
5. **Por que PostgreSQL?** É robusto, gratuito, tem a trava por dia, JSONB para a auditoria e roda igual no Docker e nos testes com Testcontainers.
6. **O que é Clean Architecture aqui?** A regra fica no `domain`, sem Spring. Os casos de uso usam interfaces (portas). Banco, e-mail e ERP são adaptadores que podem ser trocados.
7. **Como garantiu o perfil no backend?** `@PreAuthorize` nos métodos e regras por URL no Spring Security. A tela só esconde botões; quem bloqueia é a API (403).
8. **Como funciona o JWT?** O login devolve um token assinado com segredo do ambiente, contendo usuário e perfil. Cada chamada manda o token e o filtro valida. Não há sessão no servidor.
9. **E as senhas?** BCrypt. Na falha de login comparo com um hash fictício, para o tempo de resposta não revelar se o usuário existe.
10. **O que a auditoria grava?** Usuário, data/hora, ação, entidade, id, IP e detalhes em JSON. Login com sucesso e com falha também.
11. **E se o e-mail falhar?** O envio é assíncrono. O agendamento fica salvo e o erro vai para o log.
12. **Por que Mailpit?** Para testar o e-mail de verdade sem mandar para ninguém. Em produção basta trocar `SMTP_HOST`.
13. **Como é o link do fornecedor seguro?** Token aleatório de 32 bytes. No banco fica só o hash. Gerar um novo invalida o anterior, e depois da chegada o link deixa de funcionar.
14. **O que é o PWA?** O site pode ser instalado no celular, abre sem barra do navegador e mostra uma tela quando está sem internet.
15. **Push em Java funciona?** Sim. Uso a biblioteca `web-push` com chaves VAPID. O navegador entrega a notificação mesmo com a aba fechada.
16. **Quando chega push?** Novo agendamento, cancelamento, capacidade excedida, chegada na portaria e divergência.
17. **Como testou?** JUnit 5 e Mockito para a regra e os casos de uso, Testcontainers com Postgres real para a API inteira, Vitest no frontend.
18. **Qual a cobertura?** Backend: domain + application 99,6% de linhas e 91,4% de ramos; o build falha abaixo de 80%. Frontend: 95,8% de linhas e 83,7% de ramos nas telas críticas, com corte de 80% no `test:coverage`.
19. **Por que Flyway?** O banco é versionado junto com o código. Qualquer ambiente sobe com a mesma estrutura.
20. **Como estão os Dockerfiles?** Multi-stage: compila numa imagem e roda em outra pequena, sem root. O frontend roda em nginx sem root.
21. **E a segurança das dependências?** Dependency-Check configurado e Trivy nas imagens. Atualizei o Boot e as bibliotecas. Os 4 achados que restaram não se aplicam ao projeto e estão justificados em SEGURANCA.md.
22. **Por que versionar a API em /api/v1?** Se mudar o contrato, crio a v2 sem quebrar quem usa a v1.
23. **Como entra o Bluesoft?** Por uma porta `PedidoCompraGateway`. Hoje roda o adaptador local. O do Bluesoft está pronto (OAuth `client_credentials`) e liga por variável quando houver credencial.
24. **O que veio do Gestão CD?** Regras de capacidade, portaria, conferência, link do fornecedor e vale-palete com assinatura. O inventário completo está em PORTE-GESTAO-CD.md.
25. **O que faria com mais tempo?** WhatsApp como mais um canal de aviso, e-mail de cobrança de paletes, tipos de palete configuráveis e migração para o Spring Boot 4.
26. **Como é o fuso horário?** Fixo em America/Sao_Paulo na API e nas telas, para o horário da faixa não mudar.
27. **O fornecedor pode estourar a capacidade remarcando?** Não. Pelo link ele só vê como livres as faixas onde a carga cabe, e o servidor confere de novo. Quem pode exceder é o CD (só alerta), como pede o desafio.
28. **Como funciona o multi-CD sem quebrar a regra do desafio?** O CD 1 não tem capacidade própria, então usa 480/120. Um CD novo pode ter outro limite; a conta é a mesma, só muda a capacidade.
29. **Como a NF-e é lida com segurança?** Parser DOM com DTD e entidades externas desligadas (sem XXE), arquivo até 2 MB, e a chave de acesso não pode repetir.
30. **De onde vem o peso da carga?** Do cadastro de produtos (peso bruto × quantidade). A tela compara com o peso da nota e marca divergência acima de 5%. Produto sem peso gera aviso e entra no filtro "somente sem peso".
31. **Como sai o saldo de paletes do fornecedor?** Paletes entregues nos vales − devolvidos − cobrados (cobranças não canceladas). Ao receber, sai o recibo `RP-…` impresso.
32. **O que a rotina do dia mostra?** Entregas de amanhã sem confirmação, atrasados, espera acima da tolerância, descarga longa, recebidos sem NF-e, divergências, vales pendentes e cobranças vencidas.
33. **Como testou o frontend?** Vitest + Vue Test Utils montando as telas reais com PrimeVue e a API simulada: validação do formulário, envio, remarcação, cadastros, guarda de rota e login.

