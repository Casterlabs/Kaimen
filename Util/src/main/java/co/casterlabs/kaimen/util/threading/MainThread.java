package co.casterlabs.kaimen.util.threading;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class MainThread {
    private static @Getter Thread thread;

    private static Impl defaultImpl;
    private static @Deprecated @Nullable @Setter Impl impl;

    public static void park(@Nullable Runnable continued) {
        assert thread == null : "Already parked.";

        thread = Thread.currentThread();

        if (continued != null) {
            Thread cont = new Thread(continued);

            cont.setName("App Thread");
            cont.start();
        }

        final Lock lock = new Lock(); // Create the resource on THIS thread.
        final Deque<Runnable> taskQueue = new ArrayDeque<>();

        defaultImpl = (task) -> {
            taskQueue.add(task);

            synchronized (lock) {
                lock.notify();
            }
        };

        while (true) {
            while (!taskQueue.isEmpty()) {
                try {
                    Runnable popped = taskQueue.pop();

                    try {
                        popped.run();
                    } catch (Throwable t) {
                        FastLogger.logStatic(LogLevel.SEVERE, "An exception occurred whilst processing task on the main thread:");
                        FastLogger.logException(t);
                    }
                } catch (NoSuchElementException ignored) {}
            }

            try {
//                Thread.yield(); // The thread may lie dormant for a while.

                synchronized (lock) {
                    // Sleep until we get another task.
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void submitTask(@NonNull Runnable task) {
        if (isMainThread()) {
            task.run();
        } else {
            if (impl == null) {
                defaultImpl.submitTask(task);
            } else {
                impl.submitTask(task);
            }
        }
    }

    public static void submitTaskAndWait(@NonNull Runnable task) throws InterruptedException, Throwable {
        if (isMainThread()) {
            task.run();
        } else {
            Promise<Void> promise = new Promise<>();

            submitTask(() -> {
                try {
                    task.run();
                    promise.fulfill(null);
                } catch (Throwable t) {
                    promise.error(t);
                    throw t;
                }
            });

            promise.await();
        }
    }

    public static void executeOffOfMainThread(@NonNull Runnable task) {
        if (isMainThread()) {
            new AsyncTask(task);
        } else {
            task.run();
        }
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == thread;
    }

    public static interface Impl {

        public void submitTask(@NonNull Runnable task);

    }

}
