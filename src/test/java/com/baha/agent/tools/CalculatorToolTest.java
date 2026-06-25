package com.baha.agent.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    @Test
    void addsTwoNumbers() {
        assertThat(tool.calculate("2 + 2")).isEqualTo("4");
    }

    @Test
    void respectsOperatorPrecedence() {
        assertThat(tool.calculate("2 + 3 * 4")).isEqualTo("14");
    }

    @Test
    void respectsParentheses() {
        assertThat(tool.calculate("(2 + 3) * 4")).isEqualTo("20");
    }

    @Test
    void formatsNonWholeResult() {
        assertThat(tool.calculate("7 / 2")).isEqualTo("3.5");
    }

    @Test
    void largeIntegralResultIsNotNarrowedToLong() {
        // 10^20 exceeds long range; must not silently become Long.MAX_VALUE.
        assertThat(tool.calculate("10^20")).isEqualTo("1.0E20");
    }

    @Test
    void malformedExpressionReturnsClearError() {
        assertThat(tool.calculate("2 +")).startsWith("Invalid expression");
    }

    @Test
    void divisionByZeroReturnsClearError() {
        assertThat(tool.calculate("1 / 0")).startsWith("Invalid expression");
    }
}
