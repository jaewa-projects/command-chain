package com.jaewa.commandchain;

import lombok.Getter;

public class TimedLoop extends AbstractLoop {

    @Getter
    private final long duration;

    @Getter
    private long start;

    @Getter
    private int cycle = 0;

    public TimedLoop(String varName, long duration) {
        super(varName);
        this.duration = duration;
    }

    @Override
    protected void init() {
        start = System.currentTimeMillis();
    }

    @Override
    public boolean hasNext() {
        return getElapsedTime() < duration;
    }

    @Override
    public void next() {
        cycle++;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - start;
    }

}
