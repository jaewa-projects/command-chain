# Jaewa Command Chain

A lightweight Java 11+ library designed to orchestrate complex algorithms as a sequence of asynchronous steps. The core philosophy is that each step (Command) is responsible for its own completion, signaling the progression to the next phase via manual flow control.

## Key Concept: Asynchronous Step Orchestration

Unlike traditional linear execution, `command-chain` allows you to model algorithms where each step might involve asynchronous operations (I/O, timers, external events). A step is considered "finished" only when it explicitly calls `next()` on the chain controller.

## Features

- **Manual Flow Control**: Total control over algorithm progression using `chain.next()` and `chain.fail()`.
- **Asynchronous by Design**: Ideal for simulating complex state machines or multi-step processes where steps finish at different times.
- **Fluent Algorithm Builder**: Compose your logic using a clean, readable builder API.
- **Dynamic Command Addition**: Add new commands to the chain even during execution, allowing for adaptive workflows.
- **Native Loop Support**: Built-in support for repetitive tasks (`ForLoop`, `TimedLoop`) that integrate seamlessly with the asynchronous flow.
- **Robust Error Propagation**: Centralized failure handling that catches both synchronous exceptions and manual failure signals.
- **Thread Efficiency**: Optimized for non-blocking execution, utilizing threads only when necessary.

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.jaewa</groupId>
    <artifactId>command-chain</artifactId>
    <version>1.0.1</version>
</dependency>
```

---

## General Description

### CommandExecutor and Command Sources

The `CommandExecutor` is the heart of the system. It manages a collection of commands and coordinates their execution. There are two primary modes of operation based on the `CommandSource` used:

1.  **CommandPipeline (Default)**: Commands remain in the collection after being executed. This is ideal for re-running the same sequence of steps multiple times.
2.  **CommandQueue**: Commands are removed from the collection once they are executed. This is perfect for producer-consumer scenarios or background workers where tasks are processed and then discarded.

### Commands: Async vs Sync

The library distinguishes between two types of commands:

-   **AsyncCommand**: The core of the library. It receives a `CommandChain` object. The execution "stops" at this command until the command itself explicitly calls `chain.next()` or `chain.fail(throwable)`.
-   **Command**: A simple, synchronous step. Once the method returns, the system automatically moves to the next command.
  If the command throws an exception, the `fail()` method of the CommandChain is automatically called with that
  exception.

#### Why AsyncCommand?

Async commands are essential for operations that take time and finish asynchronously, such as calling an external REST API, waiting for a database query, or a user interaction.

```java
(ctx, chain) -> {
    externalService.callAsync(data)
        .thenAccept(result -> {
            ctx.set("result", result);
            chain.next(); // Continue to next step ONLY when API returns
        })
        .exceptionally(ex -> {
            chain.fail(ex); // Signal failure if API fails
            return null;
        });
}
```

**Advantages**:
-   **No Blocked Threads**: The system does not use any thread while waiting for the external API to complete. No thread is put in `wait()` state.
-   **Resource Efficiency**: You can handle thousands of concurrent chains with a very small thread pool.

---

## Fluent Builder API

The library provides a fluent builder to compose complex algorithm chains.

### Simple Chains

You can build chains using both synchronous and asynchronous commands. The `exec()` method is overloaded to accept various types.

```java
CommandExecutor.pipelineBuilder()
    // Synchronous command (auto-next)
    .exec(ctx -> System.out.println("Step 1: Sync"))
    
    // Asynchronous command (manual-next)
    .exec((ctx, chain) -> {
        System.out.println("Step 2: Async start");
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            System.out.println("Step 2: Async end");
            chain.next();
        });
    })
    
    // Elaborate AsyncCommand with CompletableFuture
    .exec((ctx, chain) -> {
        CompletableFuture<String> future = someService.fetchData();
        future.handle((res, ex) -> {
            if (ex != null) {
                chain.fail(ex);
            } else {
                ctx.set("data", res);
                chain.next();
            }
            return null;
        });
    })
    .build()
    .start(new DefaultContext());
```

### Overloaded `exec()` Behavior

The `exec()` method (available on the builder) can receive:

-   **`Command`**: Executed synchronously; progression is automatic.
-   **`AsyncCommand`**: Executed asynchronously; progression requires `chain.next()`.
-   **`CompletableFuture<?>`**: The builder wraps it automatically. The chain proceeds when the future completes.
-   **`Runnable`**: Wrapped as a synchronous command.

### Wiretap (Side-effects)

The `wiretap()` method allows you to inject side-effects into the chain without interfering with the main execution flow. It takes a `Runnable` that is executed in a parallel thread, while the system immediately moves to the next command without waiting. This is perfect for logging, metrics, or monitoring.

```java
CommandExecutor.pipelineBuilder()
    .exec(ctx -> ctx.set("status", "processing"))
    .wiretap(() -> logger.info("Status set to processing"))
    .exec(someAsyncCommand)
    .build();
```

### CommandExecutor as the Engine

The builder creates a `CommandExecutor` instance. To start the execution, you call the `start(Context)` method.

```java
CommandExecutor executor = CommandExecutor.pipelineBuilder()
    .exec(ctx -> System.out.println("Hello"))
    .build();

CompletableFuture<Void> future = executor.start(new DefaultContext());

future.thenRun(() -> System.out.println("Chain finished successfully"));
future.exceptionally(ex -> {
    // CompletableFuture wraps the original exception in a CompletionException
    Throwable originalCause = ex.getCause() != null ? ex.getCause() : ex;
    System.err.println("Chain failed: " + originalCause.getMessage());
    return null;
});
```
The `start()` method returns a `CompletableFuture` that:
-   **Completes successfully** when the entire chain finishes without errors.
-   **Completes with an exception** if `chain.fail(throwable)` is called somewhere in the chain and the error is not handled (e.g., via `onFailure` with `chain.next()` or a `doCatch` block).

Note that since `CompletableFuture` is used, the exception passed to `exceptionally` or `handle` is typically a `java.util.concurrent.ExecutionException`. You can retrieve the original error thrown by your command using `ex.getCause()`.

### Nested Executors (Sub-blocks)

A `CommandExecutor` is itself an `AsyncCommand`. This means you can pass an executor to the `exec()` method of another builder. This allows you to create "function calls" or reusable sub-blocks of logic.

```java
CommandExecutor subBlock = CommandExecutor.pipelineBuilder()
    .exec(ctx -> System.out.println("Inside sub-block"))
    .build();

CommandExecutor main = CommandExecutor.pipelineBuilder()
    .exec(ctx -> System.out.println("Main start"))
    .exec(subBlock) // subBlock runs as a command
    .exec(ctx -> System.out.println("Main end"))
    .build();

main.start(new DefaultContext());
```

---

## Context and Scoping

The `Context` (and its implementation `DefaultContext`) is a hierarchical space for variables. It acts like a programming language's scope.

### Variable Visibility

When a `CommandExecutor` starts, it creates a **new context** that encapsulates the context passed by the user or the parent executor.

-   **Read Access**: A command can read variables from its own context and all parent contexts.
-   **Write Isolation**: When a command calls `ctx.set()`, the variable is stored in the **current** context. Parent contexts are never modified.
-   **Shadowing**: If you set a variable with the same name as one in the parent, you "shadow" it within the current scope.

```java
CommandExecutor.pipelineBuilder()
    .exec(ctx -> ctx.set("var", "parent"))
    .exec(CommandExecutor.pipelineBuilder()
        .exec(ctx -> {
            System.out.println(ctx.get("var", String.class)); // Prints "parent"
            ctx.set("var", "child"); // Shadows parent var
            System.out.println(ctx.get("var", String.class)); // Prints "child"
        })
        .build())
    .exec(ctx -> {
        System.out.println(ctx.get("var", String.class)); // Still prints "parent"!
    })
    .build()
    .start(new DefaultContext());
```

---

## Error Handling

### `onFailure` Handler

You can define an error handler for the executor using `onFailure()`. The handler can receive the exception and the `CommandChain`.

-   **`chain.next()`**: "Swallows" the error. The executor completes successfully (but stops further commands in that executor).
-   **`chain.fail(ex)`**: Propagates the error or throws a new one.

```java
CommandExecutor.pipelineBuilder()
    .exec(ctx -> { throw new RuntimeException("Oops"); })
    .onFailure((ex, chain) -> {
        System.out.println("Handling error: " + ex.getMessage());
        chain.next(); // Chain finishes cleanly
    })
    .build();
```

### Advanced Error Handling: `doTry`, `doCatch`, `doFinally`

For complex logic, use the try-catch-finally constructs. These catch errors occurring within their block, including those re-thrown by internal `onFailure` handlers.

```java
CommandExecutor.pipelineBuilder()
    .doTry()
        .exec(ctx -> { throw new IOException("Disk Full"); })
    .doCatch(IOException.class)
        .exec(ctx -> System.out.println("Recovered from IO error"))
    .doFinally()
        .exec(ctx -> System.out.println("Cleanup successful"))
    .end()
    .build();
```

---

## Building Blocks

### Loops

The builder supports `loop(AbstractLoop)`. Native implementations include:

-   **`ForLoop`**: Standard iteration (init, condition, update).
-   **`TimedLoop`**: Runs for a specific duration (milliseconds).

```java
CommandExecutor.pipelineBuilder()
    .loop(new ForLoop<>("i", () -> 0, i -> i < 5, i -> i + 1))
        .exec(ctx -> {
            ForLoop<Integer> loop = ctx.get("i", ForLoop.class);
            System.out.println("Iteration: " + loop.getValue());
        })
    .end()
    .build();
```

### Choice (Conditional Branching)

The `choice()` construct allows for `when()` and `otherwise()` branches.

```java
CommandExecutor.pipelineBuilder()
    .choice()
        .when(ctx -> ctx.get("val", Integer.class) > 10)
            .exec(ctx -> System.out.println("Greater than 10"))
        .end()
        .otherwise()
            .exec(ctx -> System.out.println("Smaller or equal to 10"))
        .end()
    .end()
    .build();
```

---

## Command Decorators (`Commands` class)

The `Commands` utility class provides static decorators to wrap logic:

-   **`async(...)`**: Wraps Runnables, Commands, or CompletableFutures into an `AsyncCommand`.
-   **`onEventQueue(...)`**: Forces execution on the AWT Event Dispatch Thread (UI).
-   **`wireTap(Runnable)`**: Executes a side-effect without blocking the main chain progression.
-   **`conditional(Predicate, AsyncCommand)`**: Executes the command only if the condition is met.
-   **`named(String, Command)`**: Assigns a name for debugging/logging.
-   **`safe(AsyncCommand)`**: Wraps a command to catch exceptions and signal failure automatically.

Example:
```java
import static com.jaewa.commandchain.Commands.*;

builder.exec(onEventQueue(ctx -> label.setText("Updating UI...")))
       .exec(wireTap(() -> logger.info("Step reached")))
       .exec(named("FetchData", async(api::call)));
```

---

## Continuous Execution Mode

In continuous mode, the executor stays alive and waits for new commands even after finishing the current ones.

```java
CommandExecutor executor = new CommandExecutor();
Future<Void> status = executor.startContinuous(new DefaultContext());

// Add commands at runtime
executor.add(ctx -> System.out.println("Dynamic command 1"));

// Check status
if (status.isDone()) {
    // This happens if someone calls executor.interrupt()
}
```

The `startContinuous` method returns a `Future` that allows you to monitor the executor's lifecycle and wait for its eventual termination.

---

## Manual CommandExecutor Usage

If you prefer not to use the builder, you can configure the `CommandExecutor` manually.

### Simple Chain
```java
CommandExecutor executor = new CommandExecutor(new CommandPipeline());
executor.add(ctx -> System.out.println("Manual Step 1"));
executor.add((ctx, chain) -> {
    CompletableFuture.runAsync(() -> {
        System.out.println("Manual Step 2");
        chain.next();
    });
});
executor.start(new DefaultContext());
```

### Manual Loop
```java
CommandExecutor executor = new CommandExecutor();
ForLoop<Integer> loop = new ForLoop<>("i", () -> 0, i -> i < 3, i -> i + 1);
loop.add(ctx -> System.out.println("Manual Loop Iteration"));
executor.add(loop);
executor.start(new DefaultContext());
```

### Manual Try-Catch
```java
TryCatchCommand tryCatch = new TryCatchCommand();
tryCatch.add(ctx -> { throw new RuntimeException("Error"); });
tryCatch.doCatch(RuntimeException.class);
tryCatch.add(ctx -> System.out.println("Caught!"));
executor.add(tryCatch);
```

---

## Thread Management and Execution

The library uses an internal `ExecutorService` to manage execution. Each command is executed on the first available thread from the underlying thread pool.

### Customizing the Executor

By default, the library uses a cached thread pool. You can change the type of `Executor` used by the system via `ExecutorService.setExecutorSupplier()`:

```java
import com.jaewa.commandchain.service.ExecutorService;
import java.util.concurrent.Executors;

// Use a fixed thread pool
ExecutorService.setExecutorSupplier(() -> Executors.newFixedThreadPool(4));

// Or use virtual threads (Java 21+)
ExecutorService.setExecutorSupplier(Executors::newVirtualThreadPerTaskExecutor);
```

This flexibility allows you to tune the performance based on your environment and the nature of your commands (CPU-bound vs I/O-bound).

---
Developed with ❤️ by Jaewa.
