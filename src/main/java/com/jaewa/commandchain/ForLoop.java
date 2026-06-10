package com.jaewa.commandchain;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Getter;

public class ForLoop<T> extends AbstractLoop {

    private final Supplier<T> initializer;
    private final Predicate<T> condition;
    private final Function<T, T> updater;

    @Getter
    private T value;

    public ForLoop(String varName, Supplier<T> initializer, Predicate<T> condition, Function<T, T> updater) {
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

}
