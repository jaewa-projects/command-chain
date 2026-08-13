package com.jaewa.commandchain;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the {@link Context} interface.
 * It supports variable storage in a map and hierarchical context through a parent context.
 */
public class DefaultContext implements Context {

    private final Map<String, Object> variables = new HashMap<>();
    private boolean interrupted = false;
    private Context parent;

    /**
     * Creates a new empty DefaultContext.
     */
    public DefaultContext(){

    }

    /**
     * Creates a new DefaultContext with a parent context.
     *
     * @param parent the parent context to delegate to
     */
    DefaultContext(Context parent){
        this.parent = parent;
    }

    @Override
    public void set(String variableName, Object value) {
        variables.put(variableName, value);
    }

    @Override
    public <E> E get(String variableName, Class<E> type) {
        if(variables.containsKey(variableName)) {
            return type.cast(variables.get(variableName));
        }else if(parent != null){
            return parent.get(variableName, type);
        }
        return null;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted || (parent != null && parent.isInterrupted());
    }

    /**
     * Marks the current context as interrupted.
     * If a parent context exists, it will also be marked as interrupted.
     */
    @Override
    public void interrupt() {
        interrupted = true;
        if(parent != null){
            parent.interrupt();
        }
    }

}
