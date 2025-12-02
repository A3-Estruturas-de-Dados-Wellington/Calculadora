Trabalho A3 da Unidade Curricular Estrutura de Dados
Professor: Wellington Larcerda
Data da Entrega: 25/11/2025
Integrantes 

Alexandre Ribeiro da Silva e Silva- 12724133597
Davi Floriano Hermida - 1272413195
Eraldino Ramos Albergaria Lopes - 12724123513


Documentação do Projeto: Calculadora de Números Complexos

1. Visão Geral
Este projeto é uma aplicação desktop desenvolvida em Java (Swing) capaz de interpretar e resolver expressões matemáticas envolvendo números complexos. Além do cálculo numérico, o sistema gera e visualiza a Árvore de Sintaxe Abstrata (AST) da operação, permitindo ao usuário ver a ordem de execução.

Principais Funcionalidades:
- Operações básicas (+, -, *, /) e avançadas (^, √) com complexos.
- Funções matemáticas (Conj).
- Suporte a variáveis (x, y, z) com entrada dinâmica de valores.
- Visualização gráfica da árvore de execução (AST) em componente JTree.
- Exportação da estrutura da árvore em formato LISP (texto).
- Módulo separado para demonstração de percurso em Árvore Binária simples.

2. Arquitetura do Sistema
O sistema pode ser dividido em três partes lógicas:
1. Modelo Matemático: Representação e aritmética dos números complexos (Complex.java).
2. Lógica de Parsing (Compilador): Interpretação da string e construção da árvore (ExpressionParser.java).
3. Interface Gráfica (GUI): Interação com o usuário (CalculatorGUI.java).

3. Detalhamento das Classes
A. Núcleo Matemático (Complex.java)Esta classe é imutável e representa um número complexo da forma a + bi.
- Armazenamento: Campos real e imag (double).
- Operações Aritméticas:
    - plus, minus, times: Operações padrão.
    - divide: Implementa a divisão multiplicando pelo conjugado do denominador.
    - pow: Utiliza a forma polar (Módulo e Argumento) para calcular potências.
    - sqrt: Método estático para raiz quadrada.
- Parsing (parse(String s)):
    - Converte strings como "3+4i", "i", "-5" em objetos Complex.
    - Utiliza manipulação de strings e lógica condicional para separar partes reais e imaginárias, tratando casos de borda (apenas i, apenas real, etc.).

B. O Motor de Interpretação (ExpressionParser.java)
Esta é a classe mais complexa. Ela atua como um Parser Descendente Recursivo (Recursive Descent Parser).
Classe Interna Node: Define a estrutura da AST (Árvore de Sintaxe Abstrata) específica para o parser.
Pré-processamento: O método preprocess insere multiplicações implícitas (ex: transforma 2i em 2*i ou 3(2+i) em 3*(2+i)).
Algoritmo de Avaliação (Hierarquia de Precedência): Os métodos são chamados em ordem para respeitar a precedência matemática:
- evaluateAdditionSubtraction: Trata + e - (menor precedência).
- evaluateMultiplicationDivision: Trata * e /.
- evaluatePower: Trata potências ^.
- evaluatePrimary / evaluateUnitary: Trata parênteses (), funções (sin, log), variáveis e números literais.

Integração com Swing:
- getExecutionTree(): Converte a AST interna para DefaultMutableTreeNode, permitindo que o JTree do Java Swing a renderize.
Tratamento de Variáveis: Aceita um Map<String, Complex> para substituir incógnitas durante o cálculo.


C. Interface Gráfica (CalculatorGUI.java)
A classe principal que executa a calculadora.
- Layout: Utiliza BorderLayout e GridLayout para organizar o display e os botões.
- Abas (JTabbedPane):
    1. Calculadora: Interface padrão.
    2. Árvore: Componente JTree que mostra a estrutura hierárquica da última conta.
    3. LISP: Mostra a árvore em formato de texto aninhado (ex: (+ 2 (* 3 i))).
- Coleta de Variáveis: O método collectVariables analisa a expressão antes do cálculo. Se encontrar letras (ex: x, y), abre um JOptionPane pedindo o valor complexo para aquela variável.
- Botão Conj: Calcula o conjugado do resultado atual e atualiza a árvore.
- Botão ==: Usa a classe ExpressionComparator para comparar duas strings de expressão (atualmente compara apenas a string literal, sem avaliação semântica profunda).
