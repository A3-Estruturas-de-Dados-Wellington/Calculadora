import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class CalculatorGUI extends JFrame implements ActionListener {

    private JTextField tela;
    private JTree arvoreExecucao;
    private JScrollPane scrollArvore;
    private JPanel painelPrincipal;
    private JTabbedPane abas;
    private JTextArea lispArea;

    private final String[] botoes = {
            " ", " ", " ", "Conj", "C",
            "x", "y", "z", "(", ")",
            "7", "8", "9", "/", "*",
            "4", "5", "6", "+", "-",
            "1", "2", "3", "i", ".",
            "^", "√", "0", "=", "==",
    };

    public CalculatorGUI() {
        super("Calculadora de Complexos");
        setSize(800, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        abas = new JTabbedPane();

        painelPrincipal = criarPainelCalculadora();
        abas.add("Calculadora", painelPrincipal);

        // Árvore Swing
        arvoreExecucao = new JTree(new DefaultMutableTreeNode("Nenhuma expressão avaliada"));
        scrollArvore = new JScrollPane(arvoreExecucao);
        abas.add("Árvore", scrollArvore);

        // Aba LISP (texto)
        lispArea = new JTextArea();
        lispArea.setEditable(false);
        JScrollPane scrollLisp = new JScrollPane(lispArea);
        abas.add("LISP", scrollLisp);

        add(abas);
    }

    private JPanel criarPainelCalculadora() {
        JPanel painel = new JPanel(new BorderLayout(5, 5));

        tela = new JTextField();
        tela.setEditable(false);
        tela.setFont(new Font("Arial", Font.BOLD, 32));
        tela.setHorizontalAlignment(SwingConstants.RIGHT);
        tela.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        painel.add(tela, BorderLayout.NORTH);

        JPanel painelBotoes = new JPanel();
        painelBotoes.setLayout(new GridLayout(6, 5, 8, 8));

        for (String texto : botoes) {
            JButton botao = new JButton(texto);
            botao.setFont(new Font("Arial", Font.BOLD, 20));
            botao.addActionListener(this);

            if (Objects.equals(texto, "=")) {
                botao.setBackground(new Color(0, 128, 255));
                botao.setForeground(Color.WHITE);
            } else if (texto.length() > 1 || (!Character.isDigit(texto.charAt(0)) && !Objects.equals(texto, "."))) {
                botao.setBackground(new Color(200, 200, 200));
            }
            painelBotoes.add(botao);
        }

        painel.add(painelBotoes, BorderLayout.CENTER);
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return painel;
    }

    private Map<String, Complex> collectVariables(String expression) {
        Set<String> variableNames = new HashSet<>();
        Map<String, Complex> variableMap = new HashMap<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isLetter(c)) {
                if (c == 'i' || c == 'I') continue;
                StringBuilder sb = new StringBuilder();
                int j = i;
                while (j < expression.length() && Character.isLetter(expression.charAt(j))) {
                    sb.append(expression.charAt(j));
                    j++;
                }
                String var = sb.toString();
                variableNames.add(var);
                i = j - 1;
            }
        }

        for (String varName : variableNames) {
            String input = JOptionPane.showInputDialog(
                    this,
                    "Digite o valor complexo para a variável " + varName + " (Ex: 3+2i, -5, 1i, i)",
                    "Entrada de Variável",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input == null || input.trim().isEmpty()) {
                return null;
            }

            try {
                Complex value = Complex.parse(input);
                variableMap.put(varName, value);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Valor complexo inválido para " + varName + ". Tente novamente.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                return null;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro inesperado ao analisar " + varName + ". Tente novamente.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        return variableMap;
    }

    private void compararExpressoes() {
        String expr1 = JOptionPane.showInputDialog(this, "Digite a primeira expressão:");
        if (expr1 == null) return;

        String expr2 = JOptionPane.showInputDialog(this, "Digite a segunda expressão:");
        if (expr2 == null) return;

        try {
            ExpressionParser p1 = new ExpressionParser(expr1, new HashMap<>());
            p1.evaluate(); // build AST
            ExpressionParser p2 = new ExpressionParser(expr2, new HashMap<>());
            p2.evaluate();

            boolean iguais = p1.structurallyEquals(p2);

            JOptionPane.showMessageDialog(this,
                    iguais ? "As expressões são ESTRUTURALMENTE iguais."
                            : "As expressões NÃO são iguais.");
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao comparar expressões:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        String comando = e.getActionCommand();
        String textoAtual = tela.getText();

        if (comando.equals("C")) {
            tela.setText("");
        } else if (comando.equals("=")) {
            if (textoAtual.isEmpty()) return;

            Map<String, Complex> vars = collectVariables(textoAtual);
            if (vars == null) return;

            try {
                ExpressionParser parser = new ExpressionParser(textoAtual, vars);
                Complex resultado = parser.evaluate();
                tela.setText(resultado.toString());

                DefaultMutableTreeNode raiz = parser.getExecutionTree();
                arvoreExecucao.setModel(new DefaultTreeModel(raiz));

                // Expande toda a árvore
                for (int i = 0; i < arvoreExecucao.getRowCount(); i++) {
                    arvoreExecucao.expandRow(i);
                }

                // Também atualiza aba LISP
                String lisp = parser.getLispTree();
                lispArea.setText(lisp);

                abas.setSelectedIndex(1);

            } catch (Exception ex) {
                tela.setText("Erro");
                ex.printStackTrace();
            }

        } else if (comando.equals("i")) {
            tela.setText(textoAtual + "i");
        } else if ("xyz".contains(comando)) {
            tela.setText(textoAtual + comando);
        } else if ("log sin cos tan".contains(comando)) {
            tela.setText(textoAtual + comando + "(");
        } else if (comando.equals("==")) {
            compararExpressoes();
        }else if (comando.equals("Conj")) {
            try {
                textoAtual = tela.getText();
                if (textoAtual.isEmpty()) return;

                Map<String, Complex> vars = collectVariables(textoAtual);
                if (vars == null) return;

                ExpressionParser parser = new ExpressionParser(textoAtual, vars);
                Complex resultado = parser.evaluate();

                Complex conj = resultado.conjugate();

                tela.setText(conj.toString());

                DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("conjugado");
                raiz.add(parser.getExecutionTree());
                arvoreExecucao.setModel(new DefaultTreeModel(raiz));

                abas.setSelectedIndex(1);

            } catch (Exception ex) {
                tela.setText("Erro");
            }
        }
        else {
            tela.setText(textoAtual + comando);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorGUI().setVisible(true));
    }
}
