package com.jaewa.commandchain;

/**
 * <h2>TimedLoop</h2>
 * <p>
 * Represents an iterative loop construct that executes for a specified duration of time.
 * Unlike traditional iteration-based loops, this loop continues executing as long as the
 * elapsed time since initialization is less than the configured duration.
 * </p>
 * <p>
 * The loop tracks the start time, elapsed time, and the number of cycles (iterations) completed.
 * Each iteration increments an internal cycle counter that can be accessed to monitor progress.
 * </p>
 * <p>
 * This class extends {@link AbstractLoop} and can be used within a command chain execution
 * context. The loop instance is automatically registered in the context under the specified
 * variable name, making it accessible to commands within the loop body.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Time-based loop execution rather than iteration-based</li>
 *   <li>Tracks elapsed time since loop initialization</li>
 *   <li>Maintains a cycle counter for the number of iterations completed</li>
 *   <li>Provides access to start time, duration, and current cycle information</li>
 *   <li>Integrates seamlessly with the command chain execution framework</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create a loop that runs for 5000 milliseconds (5 seconds)
 * TimedLoop loop = new TimedLoop("myLoop", 5000);
 *
 * // Within command execution, access the loop from context
 * TimedLoop loop = (TimedLoop) context.get("myLoop");
 * System.out.println("Elapsed: " + loop.getElapsedTime() + "ms");
 * System.out.println("Cycle: " + loop.getCycle());
 * }</pre>
 *
 * @see AbstractLoop
 * @see Loop
 */
public class TimedLoop extends AbstractLoop {

    private final long duration;

    private long start;

    private int cycle = 0;

    /**
     * Constructs a new TimedLoop with the specified variable name and duration.
     * The loop will continue executing until the elapsed time exceeds the specified duration.
     *
     * @param varName  the name under which this loop instance will be registered in the context,
     *                 allowing commands within the loop to access loop information
     * @param duration the maximum duration in milliseconds for which the loop should execute
     */
    public TimedLoop(String varName, long duration) {
        super(varName);
        this.duration = duration;
    }

    /**
     * Initializes the loop by capturing the current system time as the start time.
     * This method is called automatically by the framework before the first iteration.
     * The recorded start time is used to calculate elapsed time and determine when
     * the loop should terminate.
     */
    @Override
    protected void init() {
        start = System.currentTimeMillis();
    }

    /**
     * Determines whether the loop should continue executing.
     * The loop continues as long as the elapsed time since initialization
     * is less than the configured duration.
     *
     * @return {@code true} if the elapsed time is less than the duration,
     * {@code false} otherwise, signaling loop termination
     */
    @Override
    public boolean hasNext() {
        return getElapsedTime() < duration;
    }

    /**
     * Advances the loop to the next iteration by incrementing the cycle counter.
     * This method is called automatically by the framework after each successful
     * iteration of the loop body.
     */
    @Override
    public void next() {
        cycle++;
    }

    /**
     * Calculates and returns the elapsed time since the loop was initialized.
     * The elapsed time is computed as the difference between the current system
     * time and the start time recorded during initialization.
     *
     * @return the elapsed time in milliseconds since loop initialization
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - start;
    }

    /**
     * Returns the configured duration for which this loop is scheduled to execute.
     * This is the maximum time in milliseconds that the loop will run before termination.
     *
     * @return the loop duration in milliseconds
     */
    public long getDuration() {
        return this.duration;
    }

    /**
     * Returns the system time in milliseconds when the loop was initialized.
     * This timestamp is captured during the {@link #init()} method execution
     * and is used to calculate elapsed time.
     *
     * @return the start time in milliseconds since the Unix epoch (January 1, 1970, 00:00:00 GMT)
     */
    public long getStart() {
        return this.start;
    }

    /**
     * Returns the current cycle number, representing the number of iterations
     * completed since the loop started. The cycle counter starts at 0 and is
     * incremented after each successful iteration.
     *
     * @return the current cycle number (number of completed iterations)
     */
    public int getCycle() {
        return this.cycle;
    }
}
