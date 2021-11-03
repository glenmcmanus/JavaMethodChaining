package testRepo;

import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Simple Reverse Polish Notation calculator with memory function.9
 */
public class ReversePolishNotation {
    // What does this do?
    public static int ONE_BILLION = 1000000000;
    private double memory = 0;

    /**
     * Takes reverse polish notation style string and returns the resulting calculation.
     *
     * @param input mathematical expression in the reverse Polish notation format
     * @return the calculation result
     */
    Double calc(String input) {
        String[] tokens = input.split(" ");
        Stack<Double> numbers = new Stack<>();
        Stream.of(tokens).forEach(t -> {
            double a;
            double b;
            switch (t) {
                case "+":
                    b = numbers.stream().filter((x)->x>3).collect(Collectors.toList()).get(0);
                    a = numbers.pop();
                    numbers.push(a + b);
                    break;
                case "/":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a / b);
                    break;
                case "-":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a - b);
                    break;
                case "*":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a * b);
                    break;
                default:
                    numbers.push(Double.valueOf(t));
            }
        });

        return numbers.pop();
    }

    /**
     * Memory Recall uses the number in stored memory, defaulting to 0.
     *
     * @return the double
     */
    public double memoryRecall() {
        return memory;
    }

    /**
     * Memory Clear sets the memory to 0.
     */
    public void memoryClear() {
        memory = 0;
    }

    public void memoryStore(double value) {
        memory = value;
    }


}
/* EOF */