# Jaewa Command Chain

A lightweight Java 11+ library designed to orchestrate complex algorithms as a sequence of asynchronous steps. The core philosophy is that each step (Command) is responsible for its own completion, signaling the progression to the next phase via manual flow control.

## Key Concept: Asynchronous Step Orchestration

Unlike traditional linear execution, `command-chain` allows you to model algorithms where each step might involve asynchronous operations (I/O, timers, external events). A step is considered "finished" only when it explicitly calls `next()` on the chain controller.

## Features

- **Manual Flow Control**: Total control over algorithm progression using `chain.next()` and `chain.fail()`.
- **Asynchronous by Design**: Ideal for simulating complex state machines or multi-step processes where steps finish at different times.
- **Fluent Algorithm Builder**: Compose your logic using a clean, readable builder API.
- **Dynamic Command Addition**: Add new commands to the chain even during execution, allowing for adaptive workflows.
- **Native Loop Support**: Build-in support for repetitive tasks (`ForLoop`, `TimedLoop`) that integrate seamlessly with the asynchronous flow.
- **Robust Error Propagation**: Centralized failure handling that catches both synchronous exceptions and manual failure signals.

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.jaewa</groupId>
    <artifactId>command-chain</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Direct CommandExecutor Usage

For complete control, you can instantiate `CommandExecutor` manually. This approach is particularly useful for adding commands dynamically, managing long-running processes, or building custom iterative logic.

### 1. Manual Creation and Dynamic Commands
You can create an executor and add commands as needed. Commands can be added even while the executor is running, allowing the algorithm to adapt based on runtime data.

```java
import com.jaewa.commandchain.CommandExecutor;
import static com.jaewa.commandchain.Commands.*;

CommandExecutor executor = new CommandExecutor();

// Add an asynchronous command
executor.add("Async Step", (ctx, chain) -> {
    System.out.println("Starting async work...");
    CompletableFuture.runAsync(() -> {
        // Simulate external work
        System.out.println("Async work done.");
        chain.next(); // Explicitly move to the next step
    });
});

// Add a synchronous command (automatically advances)
executor.add("Sync Step", ctx -> {
    System.out.println("Executing synchronous step");
});

// Commands can also be added while running
executor.add("Dynamic Step Adder", (ctx, chain) -> {
    executor.add("Late Arrival", ctx2 -> System.out.println("I was added late!"));
    chain.next();
});

// Start the execution
executor.start(new DefaultContext());
```

### 2. Manual Loops with `Loop` interface
You can use loop constructs like `ForLoop` or `TimedLoop` directly with the `CommandExecutor`. Each loop is itself an `AsyncCommand` that can contain its own chain of commands via the `add()` method.

```java
import com.jaewa.commandchain.ForLoop;
import com.jaewa.commandchain.CommandExecutor;

CommandExecutor executor = new CommandExecutor();

// Create a for-loop: (i=0; i<3; i++)
ForLoop<Integer> loop = new ForLoop<>("i", () -> 0, i -> i < 3, i -> i + 1);

// Add commands to be executed INSIDE the loop
loop.add("Loop Content", ctx -> {
    int currentI = loop.getValue();
    System.out.println("Iteration: " + currentI);
});

// Add the loop itself to the main executor
executor.add("My Loop", loop);

executor.add("Post Loop", ctx -> System.out.println("Loop finished!"));

executor.start(new DefaultContext());
```

### 3. Command Decorators (`Commands` class)
The `Commands` utility class provides static methods to wrap your logic and add common behaviors. Here are the available public decorators:

- **`async(Command|Runnable)`**: Wraps a synchronous command or runnable into an `AsyncCommand`.
- **`onEventQueue(AsyncCommand|Command|FailureHandler)`**: Ensures the execution happens on the AWT Event Dispatch Thread (EDT).
- **`wireTap(Runnable)`**: Executes a side-effect asynchronously without blocking the chain progression.

```java
import static com.jaewa.commandchain.Commands.*;

// Ensure UI updates happen on the EDT
executor.add("Update Label", onEventQueue(ctx -> {
    statusLabel.setText("Processing...");
}));

// Run a side-effect (like logging) without waiting for it
executor.add("Audit", wireTap(() -> {
    logger.info("Step reached at " + System.currentTimeMillis());
}));

// Wrap a simple Runnable as an async step
executor.add("Quick Task", async(() -> {
    System.out.println("Doing something simple");
}));
```

### 4. Continuous Execution Mode
In continuous mode, the executor stays alive even after completing all current commands. It waits for new commands to be added via `add()`. This is ideal for background workers or event processors.

```java
CommandExecutor executor = new CommandExecutor();
executor.startContinuous(new DefaultContext());

// The executor is now waiting for commands...

// Later, in response to a button click or network message:
executor.add("Incoming Request", ctx -> {
    System.out.println("Processing dynamic request!");
});

// You can still add complex async commands
executor.add("Async Request", (ctx, chain) -> {
    service.doWork().thenRun(chain::next);
});
```

---

## Fluent Builder API

The library provides a fluent builder to compose complex algorithm chains, including loops and centralized failure handling.

### 1. Simple Linear Chain
```java
CommandExecutor.builder()
    .exec("Initialization", ctx -> System.out.println("Starting..."))
    .exec("Data loading", ctx -> {
        ctx.set("data", "Important value");
    })
    .exec("Finish", ctx -> {
        String data = ctx.get("data", String.class);
        System.out.println("Process completed with: " + data);
    })
    .build()
    .start(new DefaultContext());
```

### 2. Loops and Complex Orchestration
The builder supports nested loops and specialized execution contexts. You can access the loop state (like the current cycle) from the shared context.

```java
import static com.jaewa.commandchain.Commands.*;
import com.jaewa.commandchain.TimedLoop;

CommandExecutor.builder()
    .exec("open camera", ctx -> camera.open())
    .loop("timelapse loop", new TimedLoop("tl", 5000L)) // Loop for 5 seconds
        .exec("camera acquire", ctx -> {
             TimedLoop loop = ctx.get("tl", TimedLoop.class);
             System.out.println("Acquiring image for cycle: " + loop.getCycle());
             camera.acquireImages();
        })
        .exec("notify ui", onEventQueue(ctx -> updateProgressUI()))
    .end()
    .exec("camera close", ctx -> camera.close())
    .onFailure(ex -> System.err.println("Error: " + ex.getMessage()))
    .build()
    .start(new DefaultContext());
```

---

## Technical Details

### The Async Command
The heart of the library is the `AsyncCommand`. When a command is executed, the algorithm pauses until the command signals completion. This allows for natural flow control in asynchronous environments.

```java
(context, chain) -> {
    // 1. Start some work (possibly asynchronous)
    // You can use any library or mechanism (CompletableFuture, Retrofit, etc.)
    doSomethingAsync()
        .thenRun(() -> {
            // 2. Explicitly move to the next step when ready
            chain.next();
        })
        .exceptionally(ex -> {
            // 3. Or signal a failure to stop the algorithm and trigger onFailure
            chain.fail(ex);
            return null;
        });
}
```

### Shared Context
A `Context` object is passed to every step, allowing you to share data across the algorithm's execution. It supports type-safe retrieval of stored values.

```java
// Set a value
ctx.set("user_id", "12345");

// Retrieve a value with type safety
String userId = ctx.get("user_id", String.class);

// Retrieve with a default value if not present
Integer score = ctx.getOrDefault("score", Integer.class, 0);

// Check for interruption
if (ctx.isInterrupted()) {
    return; // Stop processing
}
```

### Thread Management
While the focus is on asynchronous control flow, the library uses an internal `ExecutorService` to manage the execution of the chain. This ensures that even with many concurrent chains, the execution remains non-blocking and efficient.

---
Developed with ❤️ by Jaewa.
