package com.jaewa.commandchain;

import java.util.HashMap;
import java.util.Map;

public class DefaultContext implements Context {

    private final Map<String, Object> variables = new HashMap<>();
    private boolean interrupted = false;
    private Context parent;

    public DefaultContext(){

    }

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

    public void interrupt() {
        interrupted = true;
        if(parent != null){
            parent.interrupt();
        }
    }

}
