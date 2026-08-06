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

### 1. Command Sources: Pipeline vs Queue
The library supports two main types of `CommandSource` that define how commands are managed within the executor:

- **CommandPipeline (Default)**: Commands remain in the collection after being executed. This is ideal if you need to re-initialize the chain and run the same sequence multiple times.
- **CommandQueue**: Commands are removed from the collection once they are executed. This is perfect for producers-consumers scenarios or long-running workers where commands are processed and discarded.

### 2. Manual Creation and Dynamic Commands
You can create an executor specifying the desired `CommandSource`. If no source is provided, it defaults to a `CommandPipeline`.

```java
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.CommandPipeline;
import com.jaewa.commandchain.CommandQueue;
import static com.jaewa.commandchain.Commands.*;

// Create a pipeline-based executor (default)
CommandExecutor pipelineExecutor = new CommandExecutor(new CommandPipeline());
// Or simply: CommandExecutor pipelineExecutor = new CommandExecutor();

// Create a queue-based executor
CommandExecutor queueExecutor = new CommandExecutor(new CommandQueue());

// Add an asynchronous command
pipelineExecutor.add((ctx, chain) -> {
    System.out.println("Starting async work...");
    CompletableFuture.runAsync(() -> {
        // Simulate external work
        System.out.println("Async work done.");
        chain.next(); // Explicitly move to the next step
    });
});
```

### 3. Manual Loops with `Loop` interface
You can use loop constructs like `ForLoop` or `TimedLoop` directly with the `CommandExecutor`. Each loop is itself an `AsyncCommand` that can contain its own chain of commands via the `add()` method.

```java
import com.jaewa.commandchain.ForLoop;
import com.jaewa.commandchain.CommandExecutor;

CommandExecutor executor = new CommandExecutor();

// Create a for-loop: (i=0; i<3; i++)
ForLoop<Integer> loop = new ForLoop<>("i", () -> 0, i -> i < 3, i -> i + 1);

// Add commands to be executed INSIDE the loop
loop.add(ctx -> {
    int currentI = loop.getValue();
    System.out.println("Iteration: " + currentI);
});

// Add the loop itself to the main executor
executor.add(loop);

executor.add(ctx -> System.out.println("Post Loop: Loop finished!"));

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
executor.add(onEventQueue(ctx -> {
    statusLabel.setText("Processing...");
}));

// Run a side-effect (like logging) without waiting for it
executor.add(wireTap(() -> {
    logger.info("Step reached at " + System.currentTimeMillis());
}));

// Wrap a simple Runnable as an async step
executor.add(async(() -> {
    System.out.println("Doing something simple");
}));
```

### 4. Conditional execution with `ChoiceCommand`
You can use `ChoiceCommand` to implement conditional branching in your chain. Each branch is defined by a `Predicate<Context>` and contains its own sequence of commands.

```java
import com.jaewa.commandchain.ChoiceCommand;
import com.jaewa.commandchain.CommandExecutor;

CommandExecutor executor = new CommandExecutor();

ChoiceCommand choice = new ChoiceCommand();

// First branch: if 'type' is 'A'
choice.when(ctx -> "A".equals(ctx.get("type", String.class)));
choice.add(ctx -> System.out.println("Executing branch A"));

// Second branch: if 'type' is 'B'
choice.when(ctx -> "B".equals(ctx.get("type", String.class)));
choice.add(ctx -> System.out.println("Executing branch B"));

// Add the choice command to the main executor
executor.add(choice);
executor.add(ctx -> System.out.println("Final Step: Done"));

executor.start(new DefaultContext());
```

### 5. Continuous Execution Mode
In continuous mode, the executor stays alive even after completing all current commands. It waits for new commands to be added via `add()`. This is ideal for background workers or event processors.

```java
CommandExecutor executor = new CommandExecutor();
executor.startContinuous(new DefaultContext());

// The executor is now waiting for commands...

// Later, in response to a button click or network message:
executor.add(ctx -> {
    System.out.println("Processing dynamic request!");
});

// You can still add complex async commands
executor.add((ctx, chain) -> {
    service.doWork().thenRun(chain::next);
});
```

---

## Fluent Builder API

The library provides a fluent builder to compose complex algorithm chains, including loops and centralized failure handling. You can choose between a pipeline-based builder or a queue-based builder.

### 1. Simple Linear Chain
```java
// Pipeline builder (Default source): Commands are preserved
CommandExecutor.pipelineBuilder()
    .exec(ctx -> System.out.println("Starting..."))
    .build()
    .start(new DefaultContext());

// Queue builder: Commands are consumed
CommandExecutor.queueBuilder()
    .exec(ctx -> System.out.println("Executing..."))
    .build()
    .start(new DefaultContext());
```

### 2. Loops and Complex Orchestration
The builder supports nested loops and specialized execution contexts. You can access the loop state (like the current cycle) from the shared context.

```java
import static com.jaewa.commandchain.Commands.*;
import com.jaewa.commandchain.TimedLoop;

CommandExecutor.builder()
    .exec(ctx -> camera.open())
    .loop(new TimedLoop("tl", 5000L)) // Loop for 5 seconds
        .exec(ctx -> {
             TimedLoop loop = ctx.get("tl", TimedLoop.class);
             System.out.println("Acquiring image for cycle: " + loop.getCycle());
             camera.acquireImages();
        })
        .exec(onEventQueue(ctx -> updateProgressUI()))
    .end()
    .exec(ctx -> camera.close())
    .onFailure(ex -> System.err.println("Error: " + ex.getMessage()))
    .build()
    .start(new DefaultContext());
```

### 3. Conditional Branching
The builder provides a `choice()` method to handle conditional logic cleanly. You can define multiple `when()` branches and an optional `otherwise()` block.

```java
CommandExecutor.pipelineBuilder()
    .choice()
        .when(ctx -> ctx.getOrDefault("status", Integer.class, 0) > 0)
            .exec(ctx -> System.out.println("Status is positive"))
        .end()
        .when(ctx -> ctx.getOrDefault("status", Integer.class, 0) < 0)
            .exec(ctx -> System.out.println("Status is negative"))
        .end()
        .otherwise()
            .exec(ctx -> System.out.println("Status is zero"))
        .end()
    .end()
    .exec(ctx -> System.out.println("Finalizing..."))
    .build()
    .start(new DefaultContext());
```

### Error Handling with `FailureHandler`

Centralized error management is handled via the `onFailure()` method in the builder or `setFailureHandler()` on the executor. You can use a simple `FailureHandler` for synchronous logging/cleanup or an `AsyncFailureHandler` for more complex scenarios where you need control over the chain progression after an error.

#### 1. Intercepting and Rethrowing
If you want to perform an action (like logging) and then let the error propagate to the caller (completing the `CompletableFuture` exceptionally), use `chain.fail(e)`.

```java
CommandExecutor.pipelineBuilder()
    .exec(ctx -> { throw new RuntimeException("Something went wrong"); })
    .onFailure((e, chain) -> {
        System.err.println("Logging error: " + e.getMessage());
        // Propagate the failure: the executor's future will complete exceptionally
        chain.fail(e);
    })
    .build()
    .start(new DefaultContext());
```

#### 2. Handling and Recovery
If you want to handle the error and finish the execution "cleanly" (completing the `CompletableFuture` successfully), use `chain.next()`. Note that calling `next()` in a failure handler terminates the chain execution; it does not resume from the failed command.

```java
CommandExecutor.pipelineBuilder()
    .exec(ctx -> { throw new RuntimeException("Recoverable error"); })
    .onFailure((e, chain) -> {
        System.out.println("Handling error and finishing cleanly...");
        // Finish the chain execution: the executor's future will complete successfully
        chain.next();
    })
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
