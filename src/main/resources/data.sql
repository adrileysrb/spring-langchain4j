-- REGRAS (5)
INSERT INTO regras (codigo, descricao, ativo) VALUES
('VALOR_ATIPICO', 'Movimentação de valor atípico para o perfil do cliente', TRUE),
('FRACIONAMENTO', 'Fracionamento de valores para evitar limites de controle', TRUE),
('PEP', 'Cliente classificado como Pessoa Politicamente Exposta', TRUE),
('TRANSF_INTERNACIONAL', 'Transferência internacional para país de risco', TRUE),
('INCOMPATIBILIDADE_PERFIL', 'Movimentação incompatível com a capacidade financeira declarada', TRUE);

-- PRODUTO (3)
INSERT INTO produto (codigo, descricao) VALUES
('CC', 'Conta Corrente'),
('POUP', 'Poupança'),
('INVEST', 'Conta Investimento');

-- PESSOA_MONITORADA (8)
-- pep/pem/funcionario ficam guardados so no cadastro (nao aparecem no texto das analises de proposito,
-- a ideia e que uma tool via langchain4j consulte esses campos na hora de revisar o parecer)
INSERT INTO pessoa_monitorada (cpf_cnpj, nome, tipo_pessoa, pep, pem, funcionario, data_cadastro) VALUES
('11122233344', 'Maria Souza', 'PF', FALSE, FALSE, FALSE, '2023-01-15 09:00:00'),
('22233344455', 'João Pereira', 'PF', FALSE, FALSE, TRUE, '2023-03-20 09:00:00'),
('33344455566', 'Ana Lima', 'PF', TRUE, FALSE, FALSE, '2023-05-10 09:00:00'),
('44455566677', 'Carlos Mendes', 'PF', FALSE, FALSE, FALSE, '2023-08-02 09:00:00'),
('12345678000190', 'Comercial Bras Ltda', 'PJ', FALSE, FALSE, FALSE, '2022-11-01 09:00:00'),
('23456789000181', 'Import Export SA', 'PJ', FALSE, FALSE, FALSE, '2022-12-05 09:00:00'),
('55566677788', 'Fernanda Costa', 'PF', FALSE, TRUE, FALSE, '2024-02-18 09:00:00'),
('66677788899', 'Roberto Alves', 'PF', FALSE, FALSE, FALSE, '2024-04-22 09:00:00');

-- MOVIMENTOS (30)
INSERT INTO movimentos (pessoa_monitorada_id, produto_id, valor, data_movimento, tipo_lancamento) VALUES
(1, 1, 15000.00, '2026-06-01 09:00:00', 'CREDITO'),
(1, 1, 14800.00, '2026-06-03 10:15:00', 'DEBITO'),
(1, 2, 5000.00, '2026-06-10 11:00:00', 'CREDITO'),
(1, 3, 7000.00, '2026-06-15 09:00:00', 'CREDITO'),
(2, 1, 2300.50, '2026-07-05 08:30:00', 'CREDITO'),
(2, 1, 2200.00, '2026-07-06 09:00:00', 'DEBITO'),
(2, 2, 500.00, '2026-07-15 09:00:00', 'CREDITO'),
(2, 3, 10000.00, '2026-07-09 14:00:00', 'CREDITO'),
(3, 1, 45000.00, '2026-08-01 10:00:00', 'CREDITO'),
(3, 1, 44500.00, '2026-08-01 16:00:00', 'DEBITO'),
(3, 2, 3000.00, '2026-08-02 09:00:00', 'CREDITO'),
(3, 3, 20000.00, '2026-08-03 11:30:00', 'CREDITO'),
(4, 1, 1800.00, '2026-08-15 08:00:00', 'CREDITO'),
(4, 2, 900.00, '2026-08-16 09:00:00', 'DEBITO'),
(4, 1, 1750.00, '2026-08-20 10:00:00', 'CREDITO'),
(4, 3, 2200.00, '2026-08-18 09:00:00', 'CREDITO'),
(5, 1, 120000.00, '2026-08-05 09:00:00', 'CREDITO'),
(5, 1, 118000.00, '2026-08-06 09:30:00', 'DEBITO'),
(5, 3, 60000.00, '2026-08-09 10:00:00', 'CREDITO'),
(5, 3, 59000.00, '2026-08-11 10:30:00', 'DEBITO'),
(6, 1, 85000.00, '2026-07-20 09:00:00', 'CREDITO'),
(6, 2, 15000.00, '2026-07-22 09:30:00', 'CREDITO'),
(6, 1, 80000.00, '2026-07-25 10:00:00', 'DEBITO'),
(6, 3, 30000.00, '2026-07-28 09:00:00', 'CREDITO'),
(7, 1, 3200.00, '2026-05-10 08:00:00', 'CREDITO'),
(7, 1, 3100.00, '2026-05-14 08:30:00', 'DEBITO'),
(7, 2, 1000.00, '2026-05-20 09:00:00', 'CREDITO'),
(8, 1, 9800.00, '2026-08-16 09:00:00', 'CREDITO'),
(8, 1, 9700.00, '2026-08-17 09:30:00', 'DEBITO'),
(8, 3, 25000.00, '2026-08-17 10:00:00', 'CREDITO');

-- OCORRENCIAS (6)
INSERT INTO ocorrencias (pessoa_monitorada_id, produto_id, status, data_abertura, data_encerramento) VALUES
(1, 1, 'ENCERRADA', '2026-06-05 09:00:00', '2026-06-20 09:00:00'),
(2, 3, 'EM_ANALISE', '2026-07-10 09:00:00', NULL),
(3, 1, 'ABERTA', '2026-08-02 09:00:00', NULL),
(5, 3, 'EM_ANALISE', '2026-08-10 09:00:00', NULL),
(7, 2, 'ENCERRADA', '2026-05-15 09:00:00', '2026-05-30 09:00:00'),
(8, 1, 'ABERTA', '2026-08-18 09:00:00', NULL);

-- ALERTAS (10) - dois alertas de pessoa 1 no mesmo mes ficam na mesma ocorrencia (N:1)
INSERT INTO alertas (pessoa_monitorada_id, regra_id, data_geracao, ocorrencia_id) VALUES
(1, 1, '2026-06-04 09:00:00', 1),
(1, 2, '2026-06-06 09:00:00', 1),
(2, 3, '2026-07-09 09:00:00', 2),
(3, 1, '2026-08-01 09:00:00', 3),
(3, 4, '2026-08-03 09:00:00', 3),
(5, 3, '2026-08-09 09:00:00', 4),
(5, 5, '2026-08-11 09:00:00', 4),
(7, 2, '2026-05-14 09:00:00', 5),
(8, 4, '2026-08-17 09:00:00', 6),
(4, 1, '2026-08-20 09:00:00', NULL);

-- ANALISES (8)
-- Pareceres propositalmente informais/crus (texto de analista "no bruto"), maiores e sem revisão,
-- servindo de massa de teste para a IA que fara a melhoria/redacao final do texto.
-- Cada parecer levanta a checagem de PEP/PEM/funcionario como algo crucial e ainda pendente,
-- mas sem revelar o resultado -- esse dado mora so no cadastro (pessoa_monitorada) e sera
-- consultado depois por uma tool langchain4j na hora da IA revisar/completar o texto.
INSERT INTO analises (ocorrencia_id, analista, data_analise, parecer) VALUES
(1, 'Patricia Nunes', '2026-06-08 09:00:00', 'entrei em contato com o cliente por telefone dia 07/06 e ele confirmou que os valores movimentados sao referentes a venda de um imovel, disse que tem contrato de compra e venda assinado e vai mandar por email ainda essa semana. pelo o que ele falou faz sentido com o perfil dele mas preciso ver o documento antes de fechar, tb vou confirmar se o comprador consta como PF mesmo. AINDA NAO CONFERI se o cliente se enquadra como PEP, PEM ou é funcionario da instituicao, isso é fundamental pra definir o nivel de atencao do caso e ta pendente. deixando o caso aberto aguardando doc, se nao vier ate dia 15 vou reforcar contato. obs: valor bate com o que ta no extrato, so falta comprovar origem'),
(1, 'Patricia Nunes', '2026-06-19 09:00:00', 'cliente enviou o contrato de compra e venda do imovel junto com copia do RG do comprador, confere com a movimentacao apresentada no extrato e os valores tao dentro do esperado pra esse tipo de operacao. nao identifiquei nenhum indicio de irregularidade, o cliente ja tinha historico de operacoes parecidas em analises anteriores entao nao é a primeira vez. antes de arquivar de vez, o enquadramento de pep/pem/funcionario precisa estar confirmado no cadastro, isso é decisivo pra saber se basta o parecer usual ou se precisa de aprovacao reforcada, vou deixar essa checagem registrada como pendente de validacao final. recomendo arquivar o caso assumindo que a checagem nao aponte nada, nao ha necessidade de comunicacao ao coaf nesse momento. qualquer coisa reabre depois'),
(2, 'Rafael Tavares', '2026-07-12 09:00:00', 'ainda nao consegui falar com o cliente, liguei duas vezes e caiu na caixa postal, mandei email tb pedindo o extrato bancario de origem dos recursos pra confirmar de onde veio o dinheiro que entrou na conta. pelo padrao de movimentacao (entrada seguida de debito quase igual em menos de 24h) da pra suspeitar de passagem de recurso so que ainda é cedo pra afirmar isso, precisa investigar mais. outro ponto que preciso confirmar e que pode mudar tudo é se esse cliente tem algum enquadramento de PEP, PEM ou vinculo como funcionario da instituicao, ainda nao chequei isso e é essencial pra decidir o rito da analise. vou tentar contato de novo semana que vem e se nao responder escalo pro compliance'),
(2, 'Rafael Tavares', '2026-08-01 09:00:00', 'cliente retornou e mandou o extrato solicitado, os recursos vieram de uma conta de terceiro que segundo ele é socio em outro negocio, ainda nao ficou 100% claro a relacao entre eles e o motivo da transferencia ser justamente naquele valor redondo. vou pedir mais um documento comprovando o vinculo societario antes de decidir. tb falta validar o enquadramento de pep/pem/funcionario do cliente, isso é peca chave pra saber se o caso precisa de alcada maior. seguindo em analise, nao fechar ainda, falta pouco pra concluir acho eu'),
(3, 'Patricia Nunes', '2026-08-04 09:00:00', 'caso acabou de abrir hj, ainda nao tive tempo de olhar com calma, so dei uma passada rapida nos movimentos e percebi que teve uma entrada de 45 mil seguida de debito de praticamente o msm valor no mesmo dia, isso geralmente é padrao suspeito de estruturacao ou passagem. vou puxar o historico completo do cliente amanha e tentar contato tb, e principalmente confirmar se ele se enquadra como pep, pem ou funcionario, pq isso muda completamente o nivel de criticidade que preciso dar pro caso. por enquanto fica so essa observacao inicial mesmo, nada conclusivo ainda'),
(4, 'Rafael Tavares', '2026-08-12 09:00:00', 'cliente é pessoa juridica (comercial bras ltda) com bastante movimentacao recorrente de valores altos, o que ate certo ponto é esperado pelo porte da empresa. preciso levantar o quadro societario completo e verificar se algum dos socios ou representantes se enquadra como pep, pem ou é funcionario da instituicao, essa informacao é decisiva pra saber o nivel de diligencia que precisa ser aplicado aqui e ainda nao apurei isso direito. valor em si nao parece destoante do historico mas enquanto essa checagem nao sair fechada prefiro manter o caso em acompanhamento'),
(5, 'Patricia Nunes', '2026-05-25 09:00:00', 'depois de analisar a nota fiscal e o comprovante que o cliente trouxe presencialmente na agencia, ficou claro que a movimentacao é referente a pagamento de fornecedor de mercadoria pro comercio dele, os valores e datas batem certinho com a nota. nao vejo motivo pra continuar o caso aberto, cliente ja tem relacionamento antigo e nunca teve ocorrencia antes dessa. fiz questao de confirmar no cadastro o enquadramento de pep, pem e funcionario antes de encerrar, ja que isso seria determinante pra exigir um parecer mais robusto, mas como o restante da documentacao ja justifica bem a operacao vou seguir com o encerramento. encerrando sem necessidade de comunicar ao coaf, ficou tudo bem justificado e documentado'),
(6, 'Rafael Tavares', '2026-08-19 09:00:00', 'transferencia internacional pro exterior chamou atencao pq o pais de destino consta na lista de risco que o compliance mandou mes passado, entao mesmo o cliente sendo antigo e nao ter historico de problema preciso ser mais cuidadoso aqui. ja pedi pra ele mandar comprovante de origem dos fundos e a motivacao da transferencia (contrato, invoice, sei la) pra poder avaliar melhor, ele disse que ia mandar mas ainda nao chegou nada. tb preciso confirmar o enquadramento de pep, pem ou funcionario, isso influencia direto se vou precisar de aprovacao de uma alcada superior pra esse caso. vou dar um prazo de mais uns dias e se nao vier nada reporto pro compliance mesmo sem resposta dele');

-- PROMPT_TEMPLATES (1 versao ativa por produto)
-- As configuracoes (modelo/temperature/max_tokens) sao aplicadas de verdade na chamada,
-- por isso variam por produto: investimento usa um modelo maior e mais conservador,
-- poupanca (produto de risco menor) usa um modelo mais leve.
INSERT INTO prompt_templates (produto_id, versao, nome, prompt_sistema, prompt_usuario, modelo, temperature, max_tokens, ativo) VALUES
(1, 1, 'Revisao padrao - Conta Corrente',
 'Voce e um assistente de compliance que revisa pareceres de analistas de PLDFT (prevencao a lavagem de dinheiro e financiamento ao terrorismo). Reescreva o texto do analista de forma clara, formal e objetiva, mantendo todos os fatos, decisoes e pendencias mencionadas. Nao invente informacoes que nao estejam no texto original. Responda so com o texto revisado, em portugues, sem markdown.',
 'Produto do caso: {{produto}}\nAnalista responsavel: {{analista}}\nOcorrencia: #{{ocorrenciaId}}\n\nTexto original do parecer (rascunho do analista):\n{{parecer}}\n\nReescreva esse parecer de forma profissional e bem estruturada, adequada a um relatorio de compliance de conta corrente, preservando integralmente os fatos, decisoes e pendencias descritas pelo analista.',
 'google/gemma-4-e2b', 0.30, 2000, TRUE),
(2, 1, 'Revisao padrao - Poupanca',
 'Voce e um assistente de compliance que revisa pareceres de analistas de PLDFT (prevencao a lavagem de dinheiro e financiamento ao terrorismo). Reescreva o texto do analista de forma clara, formal e objetiva, mantendo todos os fatos, decisoes e pendencias mencionadas. Nao invente informacoes que nao estejam no texto original. Responda so com o texto revisado, em portugues, sem markdown.',
 'Produto do caso: {{produto}}\nAnalista responsavel: {{analista}}\nOcorrencia: #{{ocorrenciaId}}\n\nTexto original do parecer (rascunho do analista):\n{{parecer}}\n\nReescreva esse parecer de forma profissional e objetiva, adequada a um relatorio de compliance de conta poupanca, preservando integralmente os fatos, decisoes e pendencias descritas pelo analista.',
 'google/gemma-3-1b', 0.30, 1800, TRUE),
(3, 1, 'Revisao padrao - Investimentos',
 'Voce e um assistente de compliance que revisa pareceres de analistas de PLDFT (prevencao a lavagem de dinheiro e financiamento ao terrorismo), especializado em produtos de investimento, que exigem maior rigor regulatorio. Reescreva o texto do analista de forma clara, formal e criteriosa, mantendo todos os fatos, decisoes e pendencias mencionadas. Nao invente informacoes que nao estejam no texto original. Responda so com o texto revisado, em portugues, sem markdown.',
 'Produto do caso: {{produto}}\nAnalista responsavel: {{analista}}\nOcorrencia: #{{ocorrenciaId}}\n\nTexto original do parecer (rascunho do analista):\n{{parecer}}\n\nReescreva esse parecer de forma profissional, criteriosa e bem estruturada, adequada a um relatorio de compliance de produto de investimento, preservando integralmente os fatos, decisoes e pendencias descritas pelo analista.',
 'google/gemma-3-4b', 0.20, 3000, TRUE);
