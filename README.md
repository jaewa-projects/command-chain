# Jaewa Command Chain

A lightweight Java 11+ library designed to orchestrate complex algorithms as a sequence of asynchronous steps. The core philosophy is that each step (Command) is responsible for its own completion, signaling the progression to the next phase via manual flow control.

## Key Concept: Asynchronous Step Orchestration

Unlike traditional linear execution, `command-chain` allows you to model algorithms where each step might involve asynchronous operations (I/O, timers, external events). A step is considered "finished" only when it explicitly calls `next()` on the chain controller.

## Features

- **Manual Flow Control**: Total control over algorithm progression using `chain.next()` and `chain.fail()`.
- **Active Command Protection**: Commands can only affect the chain (`next()`, `fail()`, `add()`) while they are the currently active command; late or duplicate calls are safely ignored.
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
    <version>1.1.0</version>
</dependency>
```

---

## General Description

### CommandExecutor and Command Sources

The `CommandExecutor` is the heart of the system. It manages a collection of commands and coordinates their execution. There are two primary modes of operation based on the `CommandSource` used:

1.  **CommandPipeline (Default)**: Commands remain in the collection after being executed. This is ideal for re-running the same sequence of steps multiple times.
2.  **CommandQueue**: Commands are removed from the collection once they are executed. This is perfect for producer-consumer scenarios or background workers where tasks are processed and then discarded.

### Commands: Async vs Sync

The library provides two fundamental ways to define steps in your workflow: `AsyncCommand` and `Command`.

#### 1. AsyncCommand (Explicit Flow Control)

`AsyncCommand` is the foundational building block of the library:
```java
@FunctionalInterface
public interface AsyncCommand {
    void execute(Context ctx, CommandChain chain) throws Exception;
}
```

An `AsyncCommand` is **not asynchronous by itself**—it simply receives the execution context (`Context`) and the flow controller (`CommandChain`). However, **it is what enables the algorithm execution to become asynchronous**. The chain execution stops at this step until the command explicitly signals completion by calling `chain.next()` or reports an error via `chain.fail(throwable)`.

##### Simple AsyncCommand without Asynchrony
An `AsyncCommand` does not require background threads or `CompletableFuture`. It can simply perform a direct action and then manually advance the chain:

```java
// Simple AsyncCommand: performs an action and explicitly calls next()
AsyncCommand simpleStep = (ctx, chain) -> {
    System.out.println("Executing simple step...");
    ctx.set("status", "in_progress");
    chain.next(); // Explicitly advance to the next command
};
```

#### 2. Command (Synchronous & Automatic Flow Control)

For standard, synchronous operations where you don't need manual flow control, you can use `Command`:

```java
@FunctionalInterface
public interface Command {
    void execute(Context ctx) throws Exception;
}
```

`Command` can be used directly without dealing with the `CommandChain`:
```java
// Synchronous Command: no need to call chain.next()
Command syncStep = ctx -> {
    System.out.println("Executing synchronous step...");
    ctx.set("key", "value");
};
```

##### How Command works under the hood
When you pass a `Command` to `exec()` (or use `Commands.async(cmd)`), it is **automatically converted into an `AsyncCommand`** under the hood:
- When the `execute(ctx)` method returns normally, `chain.next()` is called automatically.
- If the method throws an exception, it is caught and `chain.fail(exception)` is called automatically.

Conceptually, the adaptation works as follows:
```java
// Behind the scenes conversion (Commands.async(cmd))
(ctx, chain) -> {
    try {
        cmd.execute(ctx);
        chain.next(); // Automatically advances on success
    } catch (Exception e) {
        chain.fail(e); // Automatically fails on exception
    }
}
```

#### 3. Asynchronous Operations with CompletableFuture

The true power of `AsyncCommand` becomes evident when performing asynchronous or non-blocking tasks (such as HTTP calls, database queries, timers, or background processing). Because the chain only progresses when `chain.next()` is invoked, you can easily delegate `chain.next()` or `chain.fail()` to asynchronous callbacks:

```java
// Using an AsyncCommand with an asynchronous service
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

Or using `handle`:
```java
(ctx, chain) -> {
    CompletableFuture<String> future = someService.fetchData();
    future.handle((res, ex) -> {
        if (ex != null) {
            chain.fail(ex); // Propagate error to the chain
        } else {
            ctx.set("data", res);
            chain.next();   // Proceed with the result
        }
        return null;
    });
}
```

**Advantages**:
-   **No Blocked Threads**: The system does not use any thread while waiting for the external API to complete. No thread is put in `wait()` state.
-   **Resource Efficiency**: You can handle thousands of concurrent chains with a very small thread pool.

#### 4. Passing CompletableFuture Directly (`Commands.async`)

If you already have a `CompletableFuture`, you don't need to manually write callback boilerplate with `handle` or `thenAccept`. You can pass it directly to `exec()` using `Commands.async(future)` (or with static import `async(future)`):

```java
import static com.jaewa.commandchain.Commands.async;

CompletableFuture<String> future = someService.fetchData();

CommandExecutor.pipelineBuilder()
    // Automatically calls chain.next() on completion, or chain.fail(e) on failure
    .exec(async(future))
    .build();
```

Under the hood, `Commands.async(future)` automatically registers completion handlers on the future:
```java
(ctx, chain) -> future.whenComplete((res, ex) -> {
    if (ex != null) {
        chain.fail(ex);
    } else {
        chain.next();
    }
});
```

#### 5. Active Command Enforcement & Single-Use Flow Control

To protect against race conditions, duplicate progression, and stray or delayed asynchronous callbacks, the `CommandExecutor` enforces strict rules on the `CommandChain`:

- **Active Command Only**: A command can only interact with the `CommandChain` (calling `next()`, `fail()`, or `add()`) while it is the **currently executing (active) command**. If a command attempts to invoke `next()`, `fail()`, or `add()` when it is no longer the active command (e.g. after the chain has already progressed or completed), the call is safely ignored.
- **Single-Use `next()` and `fail()`**: Each command execution can invoke `chain.next()` or `chain.fail()` at most once. Subsequent or duplicate calls by the same command are ignored.

```java
// Example: Delayed callbacks or duplicate calls are safely ignored
(ctx, chain) -> {
    chain.next(); // Advances the chain; this command is no longer active
    
    // Any subsequent call or late callback from this command is ignored:
    chain.next(); // Ignored
    chain.fail(new RuntimeException("Late error")); // Ignored
};
```

---

## Fluent Builder API

The library provides a fluent builder to compose complex algorithm chains.

### Simple Chains

You can build chains using synchronous commands, asynchronous commands, or wrapped `CompletableFuture` instances.

```java
import static com.jaewa.commandchain.Commands.async;

CompletableFuture<String> externalFuture = someService.fetchData();

CommandExecutor.pipelineBuilder()
    // 1. Synchronous command (Command - auto-next and auto-fail on exception)
    .exec(ctx -> System.out.println("Step 1: Sync"))
    
    // 2. Simple Asynchronous command (AsyncCommand - manual next)
    .exec((ctx, chain) -> {
        System.out.println("Step 2: Simple Async");
        ctx.set("step", 2);
        chain.next();
    })
    
    // 3. Asynchronous command with background work
    .exec((ctx, chain) -> {
        System.out.println("Step 3: Async start");
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            System.out.println("Step 3: Async end");
            chain.next();
        });
    })
    
    // 4. Elaborate AsyncCommand with CompletableFuture handling
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
    
    // 5. Directly passing a CompletableFuture via Commands.async
    .exec(async(externalFuture))
    
    .build()
    .start(new DefaultContext());
```

### `exec()` Behavior

The `exec()` method (available on the builder) can receive:

-   **`Command` (`ctx -> ...`)**: Executed synchronously. The builder automatically adapts it into an `AsyncCommand` (via `Commands.async(cmd)`) which calls `chain.next()` upon completion and `chain.fail(e)` if an exception occurs.
-   **`AsyncCommand` (`(ctx, chain) -> ...`)**: Gives explicit flow control. Progression requires calling `chain.next()`, while errors are reported via `chain.fail(e)`.
-   **`CompletableFuture<?>`** (via `Commands.async(future)` / `async(future)`): Wraps the future into an `AsyncCommand`. When the future completes normally, `chain.next()` is called automatically; when it completes exceptionally, `chain.fail(e)` is called automatically.
-   **`Runnable`** (via `Commands.async(runnable)` or `wiretap(runnable)`): Can be adapted into an `AsyncCommand` or run as an independent side-effect.

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

- **`chain.next()`**: Swallows the error and allows execution to continue without errors.
- **`chain.fail(ex)`**: Propagates the error or throws a new one.

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
-   **`logged(String, AsyncCommand)`**: Assigns a name for logging.
-   **`withTimeout(long, TimeUnit, ...)`**: Wraps a `Command` or `AsyncCommand` with a maximum execution timeout, failing the chain with `CommandTimeoutException` if it does not complete in time.
-   **`safe(AsyncCommand)`**: Wraps a command to catch exceptions and signal failure automatically.

Example:
```java
import static com.jaewa.commandchain.Commands.*;

builder.exec(onEventQueue(ctx -> label.setText("Updating UI...")))
       .exec(wireTap(() -> logger.info("Step reached")))
       .exec(logged("FetchData", async(api::call)))
       .exec(withTimeout(5, TimeUnit.SECONDS, (ctx, chain) -> {
           // Asynchronous task that must call chain.next() or fail() within 5 seconds
           api.fetchDataAsync().thenAccept(result -> {
               ctx.set("data", result);
               chain.next();
           }).exceptionally(ex -> {
               chain.fail(ex);
               return null;
           });
       }));
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
