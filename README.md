# Sistema Operacional Simulado (OS Simulator)

Aplicação desktop educacional em Java que simula:
- Escalonamento FCFS, SJF, Prioridade e Round Robin.
- Processos e métricas básicas.
- RAM dividida em quadros.
- Paginação com substituição FIFO ou LRU.
- Cache didática associativa com substituição LRU.
- Painel em tempo real, log de eventos, pausa, avanço de ciclo e reinício.

> **Aviso:** este projeto simula comportamentos. Ele não mede nem controla a CPU, a RAM ou a cache física do computador.

## Requisitos
- JDK 17 ou superior.
- Maven 3.9+ (ou configure o Maven Wrapper no seu ambiente).
- Windows, Linux ou macOS com ambiente gráfico.

## Executar
Na pasta do projeto:

```bash
mvn clean test
mvn package
mvn exec:java
```

O `exec-maven-plugin` já está configurado no `pom.xml`; `mvn exec:java` inicia a interface.

No Windows PowerShell, os comandos Maven são iguais quando `mvn` está no PATH.

## Usar
1. Abra o projeto no VS Code.
2. Execute `mvn clean test`.
3. Rode `br.com.gaozin.osimulator.Main`.
4. Escolha o algoritmo antes de iniciar.
5. Use **Iniciar**, **Pausar**, **1 ciclo** e **Reiniciar**.
6. O botão **+ Processo** permite adicionar processos antes do primeiro ciclo.
7. A velocidade muda apenas a frequência de atualização visual.

## Algoritmos
- FCFS: primeiro a chegar, primeiro a executar.
- SJF: menor duração restante entre processos prontos, sem preempção.
- Prioridade: menor número representa maior prioridade, sem preempção.
- Round Robin: quantum configurável e retorno ao final da fila.

## Modelo didático e limitações
- O relógio é discreto e avança em ciclos inteiros.
- O tamanho da RAM deve ser divisível pelo tamanho de página.
- A alocação de memória é feita em páginas inteiras, arredondando para cima.
- O módulo de paginação mantém seu próprio conjunto de páginas residentes para demonstrar FIFO/LRU.
- A cache é uma cache associativa simplificada de endereços físicos simulados.
- A CPU usa um padrão determinístico de referências a páginas; não é um interpretador de instruções reais.
- O custo de troca de contexto não é subtraído do tempo de CPU nesta versão.
- O tratamento de falta de página é contabilizado como evento, mas não simula latência de disco nem bloqueio assíncrono.
- O programa é educacional; não é um kernel nem um emulador de hardware.

## Testes
```bash
mvn test
```

Os testes cobrem validação de processos, alocação de RAM, cache LRU e contadores de paginação.

## Estrutura
- `process`: modelo de processo e estados.
- `scheduler`: algoritmos de escalonamento.
- `memory`: alocação de RAM e quadros.
- `paging`: páginas residentes e substituição.
- `cache`: hits, misses e LRU.
- `simulation`: relógio lógico e coordenação.
- `events`: log de eventos.
- `ui`: interface Swing.

## Próximas melhorias
1. Adicionar cenários salvos e exportação CSV.
2. Separar completamente o escalonador em classes de estratégia.
3. Modelar bloqueio e latência de page fault com eventos pendentes.
4. Ligar a tabela de páginas diretamente ao mapa de quadros da RAM.
5. Criar gráficos de histórico de CPU/RAM/cache.
6. Automatizar testes de integração e build no GitHub Actions.
