package com.jaewa.commandchain;

public interface Loop {
    void init(Context ctx);
    boolean hasNext();
    void next();
}
