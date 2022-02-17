package co.casterlabs.kaimen.threading;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.async.AsyncTask;
import co.casterlabs.kaimen.util.async.Lock;
import co.casterlabs.kaimen.util.async.Promise;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class MainThread {
    private static Deque<Runnable> taskQueue = new ArrayDeque<>();
    private static Thread mainThread;
    private static Lock lock;

    public static void park(@Nullable Runnable continued) {
        assert mainThread == null : "Already parked.";

        mainThread = Thread.currentThread();
        lock = new Lock(); // Create the resource on THIS thread.

        if (continued != null) {
            Thread cont = new Thread(continued);

            cont.setName("Start Thread");
            cont.start();
        }

        while (true) {

            // Process queue.
            while (!taskQueue.isEmpty()) {
                processOne();
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

    private static void processOne() {
        Runnable popped = taskQueue.pop();

        try {
            popped.run();
        } catch (Throwable t) {
            FastLogger.logStatic(LogLevel.SEVERE, "An exception occurred whilst processing task on the main thread:");
            FastLogger.logException(t);
        }
    }

    /**
     * This is here since the SWT event loop mandates special behavior.
     * 
     * @return true if the queue was processed.
     */
    @Deprecated
    public static boolean processTaskQueue() {
        assert isMainThread() : "This method may only be called on the main thread.";

        if (taskQueue.isEmpty()) {
            return false;
        } else {
            while (!taskQueue.isEmpty()) {
                processOne();
            }
            return true;
        }
    }

    public static void submitTask(@NonNull Runnable task) {
        if (isMainThread()) {
            task.run();
        } else {
            taskQueue.add(task);

            synchronized (lock) {
                lock.notify();
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
        return Thread.currentThread() == mainThread;
    }

}
