# Jaewa Command Chain

A lightweight Java 21+ library designed to orchestrate complex algorithms as a sequence of asynchronous steps. The core philosophy is that each step (Command) is responsible for its own completion, signaling the progression to the next phase via manual flow control.

## Key Concept: Asynchronous Step Orchestration

Unlike traditional linear execution, `command-chain` allows you to model algorithms where each step might involve asynchronous operations (I/O, timers, external events). A step is considered "finished" only when it explicitly calls `next()` on the chain controller.

## Features

- **Manual Flow Control**: Total control over algorithm progression using `chain.next()` and `chain.fail()`.
- **Asynchronous by Design**: Ideal for simulating complex state machines or multi-step processes where steps finish at different times.
- **Fluent Algorithm Builder**: Compose your logic using a clean, readable builder API.
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

## Core Mechanism

### 1. The Async Command
The heart of the library is the `AsyncCommand`. When a command is executed, the algorithm pauses until the command signals completion.

```java
(context, chain) -> {
    // 1. Start some work (possibly asynchronous)
    doSomethingAsync()
        .thenRun(() -> {
            // 2. Explicitly move to the next step
            chain.next();
        })
        .exceptionally(ex -> {
            // 3. Or signal a failure to stop the algorithm
            chain.fail(ex);
            return null;
        });
}
```

### 2. CommandChain Controller
The `CommandChain` object passed to each command is your handle to the algorithm's lifecycle:
- `next()`: Triggers the execution of the next command in the chain.
- `fail(Throwable)`: Immediately stops the chain and triggers the configured failure handler.

---

## Usage Examples

Below are some examples of increasing complexity to illustrate the library's potential.

### 1. Simple Linear Chain
In this example, we define a sequence of synchronous steps. Synchronous commands automatically advance to the next step.

```java
import static com.jaewa.commandchain.Commands.*;

CommandExecutor.builder()
    .exec("Initialization", ctx -> System.out.println("Starting..."))
    .exec("Data loading", ctx -> {
        ctx.set("data", "Important value");
        System.out.println("Data loaded into context.");
    })
    .exec("Finish", ctx -> {
        String data = ctx.get("data", String.class);
        System.out.println("Process completed with: " + data);
    })
    .build()
    .start();
```

### 2. Async Management and Errors
Here we introduce asynchronous commands (which must call `chain.next()` or `chain.fail()`) and a centralized failure handler.

```java
CommandExecutor.builder()
    .exec("Remote Fetch", (ctx, chain) -> {
        fetchFromApi().thenAccept(result -> {
            ctx.set("api_result", result);
            chain.next(); // Proceeds only after the async operation finishes
        }).exceptionally(ex -> {
            chain.fail(ex); // Interrupts the chain in case of error
            return null;
        });
    })
    .exec("Save", ctx -> {
        Object res = ctx.get("api_result", Object.class);
        saveToDb(res);
    })
    .onFailure(ex -> System.err.println("Error during execution: " + ex.getMessage()))
    .build()
    .start();
```

### 3. Complex Orchestration (Loops and Event Queue)
Advanced example showing integration with timed loops and command execution on the UI thread (via `onEventQueue`).

```java
import static com.jaewa.commandchain.Commands.*;

CommandExecutor.builder()
    .exec("start acquisition", onEventQueue((Context ctx) -> assignStatus(AcquireStatus.STARTING)))
    .exec("open camera", ctx -> camera.open())
    .loop("timelapse loop", new TimedLoop("loop", 5000L)) // Loop for 5 seconds
        .exec("camera acquire", ctx -> camera.acquireImages())
        .exec("save images", ctx -> {
            int cycle = ctx.get("loop", TimedLoop.class).getCycle();
            saveImages(cycle);
        })
        .exec("notify ui", onEventQueue(ctx -> updateProgressUI()))
    .end()
    .exec("camera close", ctx -> camera.close())
    .exec("go to idle", onEventQueue(ctx -> setIdleStatus()))
    .onFailure(onEventQueue(this::handleError))
    .build()
    .start();
```

---

## Technical Details

### Virtual Threads
While the focus is on asynchronous control flow, the library leverages Java 21 **Virtual Threads** to manage the execution of the chain. This ensures that even with many concurrent chains, the overhead remains minimal.

### Shared Context
A `Context` object is passed to every step, allowing you to share data across the algorithm's execution:
```java
ctx.set("result", 42);
Integer val = ctx.get("result", Integer.class);
```

---
Developed with ❤️ by Jaewa.
