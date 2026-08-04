package com.jaewa.commandchain;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A generic implementation of a for-loop construct that allows initialization, condition
 * checking, and updating logic to be specified using functional interfaces. The class extends
 * the {@link AbstractLoop}, providing custom loop behavior within a context-aware environment.
 *
 * <p>
 * This class simulates the functioning of a traditional for loop by breaking it down into
 * three distinct phases: initialization, condition evaluation, and update. The loop begins
 * by initializing a variable using the provided {@link Supplier}, then repeatedly checks
 * the condition via a {@link Predicate}, and updates the variable using a {@link Function}
 * after each iteration. The loop continues as long as the condition evaluates to true,
 * mimicking the behavior of a standard for loop construct.
 * </p>
 *
 * <p>
 * The ForLoop instance is registered in the {@link Context} using the variable name specified
 * during construction. This allows any command in the execution chain to retrieve the ForLoop
 * instance from the Context and access its current state, including the current loop value via
 * {@link #getValue()}.
 * </p>
 *
 * @param <T> the type of the variable being iterated on
 */
public class ForLoop<T> extends AbstractLoop {

    private final Supplier<T> initializer;
    private final Predicate<T> condition;
    private final Function<T, T> updater;

    private T value;

    /**
     * Constructor for the ForLoop class, which represents a generic implementation of a for-loop
     * construct using functional interfaces for initialization, condition checking, and iteration logic.
     *
     * @param varName     the name used to register this ForLoop instance in the Context, allowing
     *                    any command to retrieve the loop instance and access its state during execution
     * @param initializer a {@link Supplier} that provides the initial value of the loop variable
     * @param condition   a {@link Predicate} that evaluates whether the loop should continue iterating
     * @param updater     a {@link UnaryOperator} that updates the loop variable after each iteration
     */
    public ForLoop(String varName, Supplier<T> initializer, Predicate<T> condition, UnaryOperator<T> updater) {
        super(varName);
        this.initializer = initializer;
        this.condition = condition;
        this.updater = updater;
    }

    @Override
    protected void init() {
        value = initializer.get();
    }

    @Override
    public boolean hasNext() {
        return condition.test(value);
    }

    @Override
    public void next() {
        value = updater.apply(value);
    }

    /**
     * Retrieves the current value of the loop variable.
     *
     * @return the current value being managed by the loop
     */
    public T getValue() {
        return this.value;
    }
}
