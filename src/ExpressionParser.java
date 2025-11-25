import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.util.HashMap;

public class ExpressionParser {

    private String expression;
    private int position;
    private final Map<String, Complex> variables;
    private final Map<String, Complex> allVariables;

    private Node root;
    private Complex lastResult;

    // AST interno
    private static class Node {
        String value;
        Node left, right;

        Node(String value) { this.value = value; }
        Node(String value, Node left, Node right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }
        @Override public String toString() { return value; }
    }

    public ExpressionParser(String expression, Map<String, Complex> variables) {
        this.expression = preprocess(expression == null ? "" : expression.replaceAll("\\s+", ""));
        this.position = 0;
        this.variables = variables == null ? new HashMap<>() : variables;

        this.allVariables = new HashMap<>(this.variables);
        this.allVariables.put("i", new Complex(0, 1));
    }

    private String preprocess(String expr) {
        if (expr == null || expr.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < expr.length() - 1; i++) {
            char c1 = expr.charAt(i);
            char c2 = expr.charAt(i + 1);
            out.append(c1);
            if (needsMultiplication(c1, c2)) out.append('*');
        }
        out.append(expr.charAt(expr.length() - 1));
        return out.toString();
    }

    private boolean needsMultiplication(char c1, char c2) {
        return (Character.isDigit(c1) && (Character.isLetter(c2) || c2 == '(')) ||
                (c1 == ')' && (Character.isDigit(c2) || Character.isLetter(c2) || c2 == '(')) ||
                (Character.isLetter(c1) && (Character.isDigit(c2) || c2 == '('));
    }

    public Complex evaluate() {
        root = null;
        position = 0;
        lastResult = evaluateAdditionSubtraction();

        if (position != expression.length()) {
            throw new IllegalArgumentException("Erro ao analisar expressão próximo de: " + expression.substring(position));
        }
        return lastResult;
    }

    private Node makeNode(String value, Node left, Node right) {
        return new Node(value, left, right);
    }

    private Complex evaluateAdditionSubtraction() {
        Node leftNode = null;
        Complex result = evaluateMultiplicationDivision();
        leftNode = root;

        while (position < expression.length()) {
            char op = expression.charAt(position);
            if (op == '+' || op == '-') {
                position++;
                Complex right = evaluateMultiplicationDivision();
                Node rightNode = root;

                root = makeNode(String.valueOf(op), leftNode, rightNode);

                if (op == '+') result = result.plus(right);
                else result = result.minus(right);

                leftNode = root;
            } else break;
        }
        return result;
    }

    private Complex evaluateMultiplicationDivision() {
        Node leftNode = null;
        Complex result = evaluatePower();
        leftNode = root;

        while (position < expression.length()) {
            char op = expression.charAt(position);
            if (op == '*' || op == '/') {
                position++;
                Complex right = evaluatePower();
                Node rightNode = root;

                root = makeNode(String.valueOf(op), leftNode, rightNode);

                if (op == '*') result = result.times(right);
                else result = result.divide(right);

                leftNode = root;
            } else break;
        }
        return result;
    }

    private Complex evaluatePower() {
        Complex result = evaluatePrimary();
        Node baseNode = root;

        while (position < expression.length() && expression.charAt(position) == '^') {
            position++;
            Complex right = evaluatePrimary();
            Node expNode = root;

            if (right.getImag() != 0) throw new IllegalArgumentException("Expoente da potência deve ser real.");
            result = result.pow(right.getReal());
            root = makeNode("^", baseNode, expNode);
            baseNode = root;
        }
        return result;
    }

    private Complex evaluatePrimary() {
        return evaluateUnitary();
    }

    private Complex evaluateUnitary() {
        boolean isNegative = false;
        if (position < expression.length() && expression.charAt(position) == '-') {
            isNegative = true;
            position++;
        }

        Complex result;

        if (position < expression.length() && Character.isLetter(expression.charAt(position))) {
            int start = position;
            while (position < expression.length() && Character.isLetter(expression.charAt(position))) position++;
            String name = expression.substring(start, position);

            if (position < expression.length() && expression.charAt(position) == '(') {
                position++;
                Complex arg = evaluateAdditionSubtraction();
                if (position < expression.length() && expression.charAt(position) == ')') position++;
                else throw new IllegalArgumentException("Parênteses da função não fechados.");

                Node argNode = root;
                root = makeNode(name, argNode, null);

                if (arg.getImag() != 0) {
                    throw new IllegalArgumentException("Funções trigonométricas/log para parte imaginária não suportadas.");
                }
                double v = arg.getReal();
                switch (name) {
                    case "sin": result = new Complex(Math.sin(v), 0); break;
                    case "cos": result = new Complex(Math.cos(v), 0); break;
                    case "tan": result = new Complex(Math.tan(v), 0); break;
                    case "log":
                        if (v <= 0) throw new IllegalArgumentException("log de número não-positivo.");
                        result = new Complex(Math.log(v), 0); break;
                    default:
                        throw new IllegalArgumentException("Função desconhecida: " + name);
                }
            } else {
                String varName = name;
                if (!allVariables.containsKey(varName))
                    throw new IllegalArgumentException("Variável desconhecida: " + varName);
                result = allVariables.get(varName);
                root = new Node(varName);
            }
        }
        else if (expression.substring(position).startsWith("√")) {
            position++;
            result = evaluateUnitary();
            Node child = root;
            result = result.pow(0.5);
            root = new Node("√", child, null);
        }
        else if (position < expression.length() && expression.charAt(position) == '(') {
            int save = position;
            int start = position + 1;
            int balance = 1;
            int end = -1;
            for (int i = start; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') balance++;
                if (c == ')') balance--;
                if (balance == 0) { end = i; break; }
            }
            if (end != -1) {
                String content = expression.substring(start, end);
                try {
                    Complex lit = Complex.parse(content);
                    position = end + 1;
                    result = lit;
                    root = new Node(content);
                } catch (Exception ex) {
                    position = save;
                    position++;
                    result = evaluateAdditionSubtraction();
                    if (position < expression.length() && expression.charAt(position) == ')') position++;
                    else throw new IllegalArgumentException("Parênteses não fechados.");
                }
            } else {
                throw new IllegalArgumentException("Parênteses não fechados.");
            }
        }
        else if (position < expression.length() && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
            int start = position;
            while (position < expression.length() &&
                    (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) position++;
            String num = expression.substring(start, position);
            if (num.isEmpty()) throw new IllegalArgumentException("Número esperado.");
            result = new Complex(Double.parseDouble(num), 0);
            root = new Node(num);
        }
        else if (position < expression.length() && (expression.charAt(position) == 'i' || expression.charAt(position) == 'I')) {
            position++;
            result = new Complex(0, 1);
            root = new Node("i");
        }
        else {
            throw new IllegalArgumentException("Operando esperado em posição " + position);
        }

        if (isNegative) {
            result = result.scale(-1);
            root = makeNode("-", new Node("0"), root);
        }
        return result;
    }

    public DefaultMutableTreeNode getExecutionTree() {
        String resultLabel = (lastResult != null) ? "Resultado: " + lastResult.toString() : "Resultado: (vazio)";
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(resultLabel);
        DefaultMutableTreeNode exprTree = buildSwingTree(root);
        top.add(exprTree);
        return top;
    }

    private DefaultMutableTreeNode buildSwingTree(Node n) {
        if (n == null) return new DefaultMutableTreeNode("vazio");
        String label = n.value;
        if (variables != null && variables.containsKey(n.value)) {
            Complex val = variables.get(n.value);
            label = n.value + " = " + val.toString();
        } else {
            try {
                Complex c = Complex.parse(n.value);
                label = c.toString();
            } catch (Exception ignored) { }
        }
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);
        if (n.left != null) treeNode.add(buildSwingTree(n.left));
        if (n.right != null) treeNode.add(buildSwingTree(n.right));
        return treeNode;
    }

    public String getLispTree() {
        if (root == null) return "";
        return buildLisp(root);
    }

    private String buildLisp(Node n) {
        if (n == null) return "";
        if (n.left == null && n.right == null) {
            return n.value;
        }
        if (n.left != null && n.right == null) {
            return "(" + n.value + " " + buildLisp(n.left) + ")";
        }
        return "(" + n.value + " " + buildLisp(n.left) + " " + buildLisp(n.right) + ")";
    }

    public boolean structurallyEquals(ExpressionParser other) {
        return compareNodes(this.root, other.root);
    }

    private boolean compareNodes(Node a, Node b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (!a.value.equals(b.value)) return false;
        return compareNodes(a.left, b.left) && compareNodes(a.right, b.right);
    }
}
