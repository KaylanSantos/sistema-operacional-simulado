# Regras da simulação

1. PID é único e positivo.
2. Chegada não pode ser negativa; duração e memória devem ser positivas.
3. Quantum deve ser positivo.
4. RAM e tamanho de página devem ser positivos, e a RAM deve ser divisível pelo tamanho de página.
5. A alocação reserva quadros inteiros; o uso contabilizado é arredondado ao próximo múltiplo do tamanho da página.
6. Prioridade 1 é a mais alta.
7. FCFS e Round Robin usam fila FIFO.
8. SJF escolhe o menor tempo restante entre os processos prontos.
9. Page faults e cache misses são métricas distintas.
10. A velocidade da interface não muda o número de ciclos lógicos.
