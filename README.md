# spring-langchain4j — PLD/FT com langchain4j

Exemplo de referência de uso do **langchain4j** com Spring Boot, construído sobre um domínio
real de **PLD/FT** (prevenção à lavagem de dinheiro e ao financiamento do terrorismo): alertas,
ocorrências, movimentações e pareceres de analistas.

Tudo é feito com a **API de baixo nível** do langchain4j — `ChatModel`, `ToolSpecification`,
`ChatRequest`, `ResponseFormat`, `ChatMemory`, `EmbeddingStore` — sem `AiServices`. A intenção é
justamente deixar visível o que a abstração de alto nível esconde: quem monta o prompt, quem
executa a tool, quem devolve o resultado pro modelo, quem valida a saída.

O provedor padrão é o **LM Studio** local (endpoint compatível com a API da OpenAI), mas trocar
por Ollama, OpenAI ou Azure é mudança de `application.properties`, não de código.

---

## As capacidades, uma a uma

Cada capacidade mora num pacote próprio em `client/llm/`, com a infraestrutura reutilizável, e é
usada pelo domínio em `client/pldft/`. Essa separação é o eixo do projeto: `llm/` não conhece
PLD/FT, e `pldft/` não conhece detalhe de provedor.

| # | Capacidade | Pacote | Padrão | Endpoint |
|---|-----------|--------|--------|----------|
| 1 | Configuração externalizada | `llm/config` | Configuration Properties | — |
| 2 | Criação e cache de modelos | `llm/model` | Factory + Value Object | — |
| 3 | Fallback entre modelos | `llm/model` | **Decorator** sobre `ChatModel` | — |
| 4 | Observabilidade de tokens/latência | `llm/observability` | **Observer** (`ChatModelListener`) | `GET /api/llm/metricas`, `GET /api/llm/chamadas` |
| 5 | Tool calling | `llm/tool` | Registry + **Strategy** (`ToolLoopPolicy`) | — |
| 6 | Tools de domínio | `pldft/tool` | Command + Factory (`OcorrenciaTools`) | — |
| 7 | Saída estruturada (JsonSchema) | `llm/structured` | **Template Method** (`StructuredExtractor`) | `POST /api/ocorrencias/{id}/classificacoes`, `POST /api/analises/{id}/pendencias` |
| 8 | Validação da saída | `llm/guardrail` | **Chain of Responsibility** | — |
| 9 | Memória de conversa | `llm/memory` | Adapter (`ChatMemoryStore` → JDBC) | `POST /api/ocorrencias/{id}/assistente` |
| 10 | RAG sobre normativos | `llm/rag` | **Facade** sobre o pipeline | `GET /api/normativos/busca`, `POST /api/normativos/pergunta` |
| 11 | Streaming (SSE) | `llm/stream` | Adapter (`StreamingChatResponseHandler` → `SseEmitter`) | `POST /api/analises/{id}/melhorias/stream` |
| 12 | Duas abordagens sob feature flag | `pldft/enquadramento` | **Strategy** + `@ConditionalOnProperty` | — |
| 13 | Revisão de parecer | `pldft/service` | composição de 4, 7, 8, 11, 12 | `POST /api/analises/{id}/melhorias` |

### 1–3. Configuração e modelos (`llm/config`, `llm/model`)

`LlmProperties` tira do código a URL, a chave, os nomes de modelo, timeouts e penalidades.
`ModelSpec` é um *value object* que também serve de chave de cache: duas partes do sistema que
pedem a mesma configuração compartilham a mesma instância.

`FallbackChatModel` é a demonstração mais direta de que `ChatModel` é só uma interface: ele
decora outro modelo e, se a chamada falhar, repete no modelo padrão. Resolve um problema real
aqui — cada produto tem um modelo configurado no banco, e raramente todos estão carregados no
LM Studio ao mesmo tempo.

### 4. Observabilidade (`llm/observability`)

`LlmCallRecordingListener` implementa `ChatModelListener` e é plugado automaticamente pelas
factories em todo modelo criado. Nenhum serviço sabe que ele existe. O mapa `attributes()` liga o
`onRequest` ao `onResponse` da mesma chamada — é por ele que o instante inicial atravessa.

`LlmTaskContext` (um `ThreadLocal`, na linha do MDC) resolve o que o listener não teria como
saber sozinho: se aquela requisição era uma revisão de parecer ou uma classificação de risco.

Falha ao gravar a métrica é engolida e logada — instrumentação não pode derrubar o que observa.

### 5–6. Tool calling (`llm/tool`, `pldft/tool`)

`ToolLoopRunner` é o loop escrito à mão: manda a conversa com as specs das tools, recebe uma
`AiMessage` que pode conter pedidos de execução, executa cada um localmente, devolve os
resultados como `ToolExecutionResultMessage` e chama o modelo de novo.

Quando parar é uma `ToolLoopPolicy`:

- `ateOModeloParar()` — `ToolChoice.AUTO`, para quando o modelo responde em texto. É o que uma
  conversa precisa.
- `ateChamarTodasAsTools()` — `ToolChoice.REQUIRED`, insiste até todas responderem. Existe porque
  modelo local pequeno às vezes *finge* ter chamado a ferramenta, escrevendo texto solto em vez de
  emitir um `tool_call`. Quando o dado é obrigatório, essa decisão não pode ficar com o modelo.

As seis tools de domínio (`PEP`, `PEM`, funcionário, movimentações, alertas, histórico) são
agrupadas por fluxo em `OcorrenciaTools`. Expor todas em todo lugar seria o caminho fácil e o
errado: prompt maior e mais chance do modelo escolher a ferramenta errada.

Detalhe deliberado em `MovimentoConsultaTool`: os totais são somados **em Java**. Somar dezenas de
lançamentos é exatamente o tipo de tarefa em que um LLM erra; a tool entrega o número pronto e
deixa pro modelo só a parte que ele faz bem.

### 7. Saída estruturada (`llm/structured`)

`StructuredExtractor` fixa o algoritmo — montar prompt, chamar com o schema, tratar falha de
formato, desserializar — e deixa pro extrator concreto só o que varia. Dois extratores provam que
a abstração não foi moldada em cima de um caso só: classificação de risco (um registro) e
pendências (uma lista tipada).

`StructuredOutputClient` carrega duas defesas que o caminho feliz não precisa e a vida real sim:

1. se o provedor não aceitar `response_format: json_schema`, a chamada é repetida com o schema
   descrito em linguagem natural dentro do prompt (`JsonSchemaDescriber`);
2. o texto é limpo antes do parse — modelo pequeno adora embrulhar o JSON em cerca de markdown.

Os valores de enum ainda são revalidados em Java antes do insert: **schema restringe o modelo, não
garante**.

### 8. Guardrails (`llm/guardrail`)

Cadeia de validações sobre o texto gerado. `Severidade` separa os dois modos de falha: `BLOQUEIA`
invalida a saída, `AVISA` registra a ressalva e deixa passar. Sem essa distinção você ou rejeita
demais e derruba o fluxo, ou loga tudo e deixa passar saída quebrada.

Regras genéricas: `NaoVazio` (sintoma de `max_tokens` estourado), `SemRecusa` (a recusa chega como
HTTP 200 e seria gravada como se fosse o parecer), `TamanhoMinimo` (resposta curta = resumo, não
reescrita), `SemMarkdown` (só avisa).

`MencionaEnquadramentoGuardrail` fica em `pldft/guardrail` porque pega o erro mais caro deste
fluxo: a tool consultou o cadastro, o dado real entrou no prompt, e o modelo reescreveu o texto
mantendo a verificação como "pendente" — saída plausível, errada justamente onde a automação
deveria ajudar.

São funções puras sobre texto, então `GuardrailChainTest` cobre todos os casos de borda em
milissegundos, sem modelo nenhum.

### 9. Memória (`llm/memory`)

`JdbcChatMemoryStore` implementa a interface `ChatMemoryStore` sobre uma tabela. A implementação
que vem pronta perde tudo ao reiniciar — aceitável num exemplo, errado quando a conversa é
evidência de auditoria. Uma linha por mensagem, serializada com `ChatMessageSerializer` para
preservar o tipo e os tool calls.

`AssistenteService` é o contraponto de todo o resto do projeto: nos outros fluxos a aplicação
dita o roteiro; aqui a conversa conduz, e o código só impõe limites — janela de memória, teto de
rodadas, guardrails na saída.

### 10. RAG (`llm/rag`)

`KnowledgeBase` esconde as quatro etapas (carregar, dividir, embarcar, indexar) atrás de uma
busca. A indexação é **preguiçosa**: o modelo de embeddings vive fora da aplicação e pode não
estar carregado; indexar na subida acoplaria o boot a um serviço opcional.

O ponto central está no prompt de `NormativoService`: o modelo é proibido de responder pelo que
"sabe" e obrigado a admitir quando o material não cobre a pergunta. Sem esse limite o RAG vira
enfeite — o modelo completa a lacuna com memória de treino, e alucinação sobre norma é o erro que
ninguém percebe até dar problema. A resposta vem sempre com as fontes.

> Os arquivos em `resources/normativos/` são **resumos didáticos reescritos para demonstração**,
> não o texto oficial das normas.

### 11. Streaming (`llm/stream`)

`SseStreamingHandler` adapta `StreamingChatResponseHandler` para `SseEmitter`, emitindo eventos
`token`, `fim` e `erro`.

Streaming muda o que a validação consegue fazer: no modo bloqueante dá pra validar tudo antes de
alguém ver; aqui os tokens já saíram. Guardrail em streaming protege o que será **persistido**,
não o leitor — por isso roda em `onCompleteResponse`, e um resultado reprovado vira evento de erro
em vez de ser gravado como se estivesse bom.

### 12. Duas abordagens sob feature flag (`pldft/enquadramento`)

O enquadramento (PEP/PEM/funcionário) é o dado que a revisão do parecer precisa resolver. Há duas
maneiras de obtê-lo, selecionáveis por `pldft.enquadramento-via-tools`:

| | `false` (**padrão**) | `true` |
|---|---|---|
| Implementação | `EnquadramentoDireto` | `EnquadramentoViaTools` |
| Como obtém | uma query ao cadastro | o modelo chama uma tool por aspecto |
| Chamadas de inferência | 0 | 1 a 3 |
| Tokens | 0 | centenas |
| Pode falhar por culpa do modelo | não | sim |

A escolha do padrão merece explicação, porque contraria o instinto de um projeto que existe para
demonstrar langchain4j: **o enquadramento é sempre necessário e vem de uma consulta determinística
— não há decisão a tomar.** Roteirizar isso pelo modelo custa inferência, adiciona latência e
introduz um modo de falha que a query não tem: modelo local pequeno às vezes *finge* ter chamado a
ferramenta, escrevendo "consultei o cadastro e..." sem nunca emitir um `tool_call`. É daí que veio
`ToolChoice.REQUIRED` e a política que insiste até as três tools responderem — uma defesa cujo
único propósito é compensar um problema que só existe porque delegamos a decisão.

Então a flag existe para tornar a comparação **concreta em vez de teórica**. Ligue e rode o mesmo
endpoint: `GET /api/llm/metricas` mostra a tarefa `CONSULTA_ENQUADRAMENTO` aparecendo, com os
tokens e a latência que ela custa. Desligue e ela some.

A lição não é "tools são ruins". É que **tool calling paga quando o modelo precisa decidir o que
consultar** — como no assistente do analista, onde a pergunta é livre e a aplicação não tem como
saber de antemão se a resposta depende de movimentações, de alertas ou do histórico. Quando a
aplicação já sabe qual dado buscar, tool calling é cerimônia.

A seleção usa `@ConditionalOnProperty`, então existe exatamente um bean de `EnquadramentoStrategy`
por vez e `AnaliseMelhoriaService` não sabe qual recebeu. `Enquadramento` centraliza a redação para
que as duas abordagens produzam o mesmo vocabulário — formato divergente tornaria a comparação
inútil, já que a diferença de resultado poderia vir do texto e não da abordagem.

Os testes cobrem os dois lados: `EnquadramentoDireto` recebe `null` no lugar do `ChatModel` (se
tocasse no modelo, explodiria) e `EnquadramentoViaToolsWiringTest` sobe a aplicação inteira com a
flag ligada, para que a configuração alternativa não vire caminho morto.

---

## Como rodar

**Pré-requisitos:** JDK 21 e [LM Studio](https://lmstudio.ai/) com o servidor local ativo em
`http://127.0.0.1:1234`.

Carregue no LM Studio:

- um modelo de chat (padrão: `google/gemma-4-e2b`);
- um modelo de embeddings, se for usar o RAG (padrão:
  `text-embedding-nomic-embed-text-v1.5`).

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com H2 em memória (console em `/h2-console`,
JDBC `jdbc:h2:mem:pldft`, usuário `sa`, sem senha). `schema.sql` e `data.sql` criam e populam a
base a cada start.

A aplicação **sobe sem o LM Studio**. Só os endpoints de IA falham, com mensagem explicando o quê.

### Configuração

```properties
llm.base-url=http://127.0.0.1:1234/v1
llm.api-key=lm-studio
llm.default-model=google/gemma-4-e2b
llm.embedding-model=text-embedding-nomic-embed-text-v1.5
llm.timeout=5m
llm.fallback-to-default-model=true

# RAG
llm.rag-chunk-size=700
llm.rag-chunk-overlap=120
llm.rag-max-results=4
llm.rag-min-score=0.55

# Feature flag: false = enquadramento lido direto do cadastro (padrão)
#               true  = obtido pelo modelo via tool calling
pldft.enquadramento-via-tools=false
```

---

## Endpoints de IA

```bash
# Revisão do parecer (tools + guardrails)
curl -X POST http://localhost:8080/api/analises/1/melhorias

# A mesma revisão, token a token
curl -N -X POST http://localhost:8080/api/analises/1/melhorias/stream

# Classificação de risco (tools -> saída estruturada)
curl -X POST http://localhost:8080/api/ocorrencias/3/classificacoes

# Pendências do parecer (saída estruturada, lista tipada)
curl -X POST http://localhost:8080/api/analises/1/pendencias

# Assistente do analista, com memória
curl -X POST http://localhost:8080/api/ocorrencias/3/assistente \
  -H 'Content-Type: application/json' \
  -d '{"pergunta":"Essa pessoa é PEP? Qual o padrão das movimentações dela?"}'

# Busca semântica crua nos normativos (sem modelo no meio)
curl 'http://localhost:8080/api/normativos/busca?q=quando%20comunicar%20ao%20COAF'

# Pergunta respondida com RAG, devolvendo as fontes
curl -X POST http://localhost:8080/api/normativos/pergunta \
  -H 'Content-Type: application/json' \
  -d '{"pergunta":"Por quanto tempo alguém continua sendo PEP depois de deixar o cargo?"}'

# Quanto cada funcionalidade custou em tokens e tempo
curl http://localhost:8080/api/llm/metricas
```

---

## Notas sobre modelos locais

Boa parte das decisões deste projeto existe por causa de modelo local pequeno, e vale explicitar:

- **Fingem chamar tools.** Escrevem "consultei o cadastro e..." sem emitir `tool_call`. Daí
  `ToolChoice.REQUIRED` e a política que insiste.
- **Estouram `max_tokens` raciocinando.** A resposta volta vazia ou cortada. Daí os timeouts
  generosos e o guardrail `NaoVazio`.
- **Entram em loop repetindo trechos.** Daí `frequencyPenalty` e `presencePenalty` nas factories.
- **Nem sempre suportam `json_schema`.** Daí o plano B com o schema descrito no prompt.
- **Perdem qualidade com contexto longo.** Daí a janela curta de memória e os registries de tools
  separados por fluxo.

Contra uma API hospedada, várias dessas defesas ficam ociosas — mas nenhuma atrapalha.

---

## Testes

```bash
./mvnw test
```

Os testes não dependem do LM Studio: cobrem a cadeia de guardrails (lógica pura), a formatação e a
seleção da estratégia de enquadramento, e a subida do contexto Spring nas duas configurações da
feature flag.
