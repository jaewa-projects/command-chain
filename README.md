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

### 1. Simulating an Asynchronous Algorithm
This example shows how a sequence of steps can control the flow.

```java
CommandExecutor executor = CommandExecutor.builder()
    .exec("Step 1: Prep", (ctx, chain) -> {
        System.out.println("Preparing...");
        // Simulated delay
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(chain::next);
    })
    .exec("Step 2: Process", (ctx, chain) -> {
        System.out.println("Processing logic...");
        boolean success = performCalculation();
        if (success) {
            chain.next();
        } else {
            chain.fail(new RuntimeException("Calculation failed"));
        }
    })
    .exec("Step 3: Cleanup", ctx -> {
        // Synchronous commands are also supported and auto-advance
        System.out.println("Done.");
    })
    .build();

executor.start().join();
```

### 2. Asynchronous Loops
Loops also wait for `chain.next()` within each iteration, allowing for complex asynchronous repetitive tasks.

```java
ForLoop<Integer> forLoop = new ForLoop<>("idx", () -> 0, i -> i < 3, i -> i + 1);

CommandExecutor.builder()
    .loop("AsyncLoop", forLoop)
        .exec("Work", (ctx, chain) -> {
            int i = ctx.get("idx", ForLoop.class).getValue();
            System.out.println("Iteration " + i + " started...");
            
            // Wait for some async event before next iteration
            myService.asyncOp().thenRun(chain::next);
        })
    .end()
    .build()
    .start();
```

### 3. Error Handling
Failures can be triggered manually or caught automatically if an exception is thrown.

```java
CommandExecutor.builder()
    .exec("RiskyStep", (ctx, chain) -> {
        if (isPathBlocked()) {
            chain.fail(new Exception("Path blocked"));
        } else {
            chain.next();
        }
    })
    .onFailure(ex -> System.err.println("Algorithm stopped: " + ex.getMessage()))
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
