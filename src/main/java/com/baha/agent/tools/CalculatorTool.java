package com.baha.agent.tools;

import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Calculator tool exposed to the agent. Evaluates a basic arithmetic
 * expression. Pure sink: input expression -> result string, no side effects.
 */
@Component
public class CalculatorTool {

    @Tool(description = "Evaluate a basic arithmetic expression with + - * / and parentheses. Use for any math.")
    public String calculate(String expression) {
        try {
            double result = new ExpressionBuilder(expression).build().evaluate();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "Invalid expression: result is not a finite number (check for division by zero).";
            }
            return format(result);
        } catch (IllegalArgumentException | ArithmeticException e) {
            return "Invalid expression: " + e.getMessage();
        }
    }

    // 2^53: above this, doubles can't represent consecutive integers, and a
    // (long) cast would silently saturate/round. Only narrow within this range.
    private static final double EXACT_INTEGER_LIMIT = 9.007199254740992E15;

    private String format(double result) {
        if (result == Math.floor(result) && Math.abs(result) < EXACT_INTEGER_LIMIT) {
            return Long.toString((long) result);
        }
        return Double.toString(result);
    }
}
