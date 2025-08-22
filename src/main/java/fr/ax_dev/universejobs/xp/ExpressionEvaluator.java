package fr.ax_dev.universejobs.xp;

import java.util.Stack;
import java.util.StringTokenizer;

/**
 * A simple mathematical expression evaluator that doesn't require JavaScript engine.
 * Supports basic operations and common functions.
 */
public class ExpressionEvaluator {
    
    /**
     * Evaluate a mathematical expression with a given variable value.
     * 
     * @param expression The expression to evaluate
     * @param variableName The variable name (e.g., "level")
     * @param variableValue The value to substitute for the variable
     * @return The result of the evaluation
     */
    public static double evaluate(String expression, String variableName, double variableValue) {
        // Replace variable with value
        String expr = expression.replace(variableName, String.valueOf(variableValue));
        
        // Handle Math functions
        expr = processMathFunctions(expr);
        
        // Convert to postfix and evaluate
        return evaluatePostfix(infixToPostfix(expr));
    }
    
    /**
     * Process Math functions in the expression.
     */
    private static String processMathFunctions(String expr) {
        // Handle Math.pow(base, exponent)
        while (expr.contains("Math.pow")) {
            int start = expr.indexOf("Math.pow(");
            if (start == -1) break;
            
            int end = findMatchingParen(expr, start + 8);
            if (end == -1) break;
            
            String args = expr.substring(start + 9, end);
            String[] parts = splitArguments(args);
            if (parts.length == 2) {
                double base = evaluateSimple(parts[0].trim());
                double exponent = evaluateSimple(parts[1].trim());
                double result = Math.pow(base, exponent);
                expr = expr.substring(0, start) + result + expr.substring(end + 1);
            } else {
                break;
            }
        }
        
        // Handle Math.sqrt(x)
        while (expr.contains("Math.sqrt")) {
            int start = expr.indexOf("Math.sqrt(");
            if (start == -1) break;
            
            int end = findMatchingParen(expr, start + 9);
            if (end == -1) break;
            
            String arg = expr.substring(start + 10, end);
            double value = evaluateSimple(arg.trim());
            double result = Math.sqrt(value);
            expr = expr.substring(0, start) + result + expr.substring(end + 1);
        }
        
        // Handle Math.floor(x)
        while (expr.contains("Math.floor")) {
            int start = expr.indexOf("Math.floor(");
            if (start == -1) break;
            
            int end = findMatchingParen(expr, start + 10);
            if (end == -1) break;
            
            String arg = expr.substring(start + 11, end);
            double value = evaluateSimple(arg.trim());
            double result = Math.floor(value);
            expr = expr.substring(0, start) + result + expr.substring(end + 1);
        }
        
        // Handle Math.ceil(x)
        while (expr.contains("Math.ceil")) {
            int start = expr.indexOf("Math.ceil(");
            if (start == -1) break;
            
            int end = findMatchingParen(expr, start + 9);
            if (end == -1) break;
            
            String arg = expr.substring(start + 10, end);
            double value = evaluateSimple(arg.trim());
            double result = Math.ceil(value);
            expr = expr.substring(0, start) + result + expr.substring(end + 1);
        }
        
        // Handle power operator ^
        expr = expr.replace("^", " ^ ");
        
        return expr;
    }
    
    /**
     * Find the matching closing parenthesis.
     */
    private static int findMatchingParen(String expr, int start) {
        int count = 1;
        for (int i = start + 1; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') count++;
            else if (expr.charAt(i) == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }
    
    /**
     * Split function arguments by comma, respecting nested parentheses.
     */
    private static String[] splitArguments(String args) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenCount = 0;
        
        for (char c : args.toCharArray()) {
            if (c == ',' && parenCount == 0) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                if (c == '(') parenCount++;
                else if (c == ')') parenCount--;
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result.toArray(new String[0]);
    }
    
    /**
     * Evaluate a simple expression (for nested calculations).
     */
    private static double evaluateSimple(String expr) {
        try {
            // Try to parse as a number first
            return Double.parseDouble(expr);
        } catch (NumberFormatException e) {
            // If not a number, evaluate as expression
            return evaluatePostfix(infixToPostfix(expr));
        }
    }
    
    /**
     * Convert infix expression to postfix notation.
     */
    private static String infixToPostfix(String infix) {
        StringBuilder postfix = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        StringTokenizer tokenizer = new StringTokenizer(infix, "+-*/^() ", true);
        
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.isEmpty()) continue;
            
            if (isNumber(token)) {
                postfix.append(token).append(" ");
            } else if (token.equals("(")) {
                stack.push('(');
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    postfix.append(stack.pop()).append(" ");
                }
                if (!stack.isEmpty()) stack.pop(); // Remove '('
            } else if (isOperator(token.charAt(0))) {
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token.charAt(0))) {
                    postfix.append(stack.pop()).append(" ");
                }
                stack.push(token.charAt(0));
            }
        }
        
        while (!stack.isEmpty()) {
            postfix.append(stack.pop()).append(" ");
        }
        
        return postfix.toString();
    }
    
    /**
     * Evaluate a postfix expression.
     */
    private static double evaluatePostfix(String postfix) {
        Stack<Double> stack = new Stack<>();
        StringTokenizer tokenizer = new StringTokenizer(postfix);
        
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isOperator(token.charAt(0))) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression");
                }
                double b = stack.pop();
                double a = stack.pop();
                
                switch (token.charAt(0)) {
                    case '+' -> stack.push(a + b);
                    case '-' -> stack.push(a - b);
                    case '*' -> stack.push(a * b);
                    case '/' -> {
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        stack.push(a / b);
                    }
                    case '^' -> stack.push(Math.pow(a, b));
                }
            }
        }
        
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }
        
        return stack.pop();
    }
    
    /**
     * Check if a string represents a number.
     */
    private static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if a character is an operator.
     */
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    /**
     * Get operator precedence.
     */
    private static int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> 0;
        };
    }
}