public class ExpressionComparator {
    public static boolean areExpressionsEqual(String expr1, String expr2) {

        String e1 = expr1.replace(" ", "");
        String e2 = expr2.replace(" ", "");

        return e1.equals(e2);
    }
}

