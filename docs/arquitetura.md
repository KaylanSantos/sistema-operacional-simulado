# Arquitetura

## Camadas
- UI: recebe comandos e renderiza uma projeção do estado.
- SimulationEngine: mantém o relógio lógico e coordena os subsistemas.
- process: representa processos e estados.
- memory: controla a alocação física em quadros.
- paging: demonstra residência e substituição de páginas.
- cache: mede acessos a endereços físicos simulados.
- events: histórico cronológico para observabilidade.

## Decisões
- Java 17 como nível mínimo de compilação para compatibilidade ampla; também funciona com JDKs mais novos.
- Swing para reduzir dependências e permitir execução simples no VS Code.
- Maven e JUnit 5 para build e testes.
- Um ciclo lógico consome um ciclo de CPU quando há processo executando.
- A interface é atualizada pelo Timer do Swing na Event Dispatch Thread.
