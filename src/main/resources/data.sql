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
INSERT INTO pessoa_monitorada (cpf_cnpj, nome, tipo_pessoa, data_cadastro) VALUES
('11122233344', 'Maria Souza', 'PF', '2023-01-15 09:00:00'),
('22233344455', 'João Pereira', 'PF', '2023-03-20 09:00:00'),
('33344455566', 'Ana Lima', 'PF', '2023-05-10 09:00:00'),
('44455566677', 'Carlos Mendes', 'PF', '2023-08-02 09:00:00'),
('12345678000190', 'Comercial Bras Ltda', 'PJ', '2022-11-01 09:00:00'),
('23456789000181', 'Import Export SA', 'PJ', '2022-12-05 09:00:00'),
('55566677788', 'Fernanda Costa', 'PF', '2024-02-18 09:00:00'),
('66677788899', 'Roberto Alves', 'PF', '2024-04-22 09:00:00');

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
INSERT INTO ocorrencias (pessoa_monitorada_id, status, data_abertura, data_encerramento) VALUES
(1, 'ENCERRADA', '2026-06-05 09:00:00', '2026-06-20 09:00:00'),
(2, 'EM_ANALISE', '2026-07-10 09:00:00', NULL),
(3, 'ABERTA', '2026-08-02 09:00:00', NULL),
(5, 'EM_ANALISE', '2026-08-10 09:00:00', NULL),
(7, 'ENCERRADA', '2026-05-15 09:00:00', '2026-05-30 09:00:00'),
(8, 'ABERTA', '2026-08-18 09:00:00', NULL);

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
INSERT INTO analises (ocorrencia_id, analista, data_analise, parecer) VALUES
(1, 'Patricia Nunes', '2026-06-08 09:00:00', 'Após verificação, cliente apresentou justificativa para os valores; solicitando documentação complementar.'),
(1, 'Patricia Nunes', '2026-06-19 09:00:00', 'Documentação recebida e validada; recomendo arquivamento do caso.'),
(2, 'Rafael Tavares', '2026-07-12 09:00:00', 'Em análise: aguardando extrato bancário do cliente para confirmar origem dos recursos.'),
(2, 'Rafael Tavares', '2026-08-01 09:00:00', 'Extrato recebido; segue em análise de compatibilidade com o perfil declarado.'),
(3, 'Patricia Nunes', '2026-08-04 09:00:00', 'Caso recém aberto, análise inicial em andamento.'),
(4, 'Rafael Tavares', '2026-08-12 09:00:00', 'Cliente PJ com histórico de operações compatíveis; revisão do enquadramento PEP em curso.'),
(5, 'Patricia Nunes', '2026-05-25 09:00:00', 'Movimentação esclarecida como pagamento de fornecedor; caso encerrado sem comunicação ao COAF.'),
(6, 'Rafael Tavares', '2026-08-19 09:00:00', 'Transferência internacional em análise; solicitado comprovante de origem dos fundos.');
