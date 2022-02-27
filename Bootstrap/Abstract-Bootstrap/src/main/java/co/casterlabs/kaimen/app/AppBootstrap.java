package co.casterlabs.kaimen.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.reflections8.Reflections;
import org.reflections8.scanners.MethodAnnotationsScanner;

import co.casterlabs.kaimen.util.threading.MainThread;
import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

public class AppBootstrap {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true); // Enable assertions.

        MainThread.park(() -> {
            // Go ahead and get the access warning out of the way
            try {
                AccessHelper.makeAccessible((Method) null);
            } catch (Throwable ignored) {}
            System.err.println("\n------------ ^ Ignore that warning ^ ------------\n\n");

            // Init the framework
            App.init(args);

            // Enter into the app
            AppEntryPoint entryPoint = findEntryPoint();

            if (entryPoint.entryAnnotation.startOnMainThread()) {
                MainThread.submitTask(entryPoint::enter);
            } else {
                entryPoint.enter();
            }
        });
    }

    private static AppEntryPoint findEntryPoint() {
        Reflections reflections = new Reflections(new MethodAnnotationsScanner());

        Set<Method> entries = reflections.getMethodsAnnotatedWith(AppEntry.class);

        assert !entries.isEmpty() : "Could not find the entry point of the app, something is definitely wrong!";
        assert entries.size() == 1 : "Found more than one app entry was found, something is definitely wrong!";

        Method entry = entries.iterator().next();
        AppEntry annotation = entry.getAnnotation(AppEntry.class);

        return new AppEntryPoint(annotation, entry);
    }

}

@AllArgsConstructor
class AppEntryPoint {
    AppEntry entryAnnotation;
    Method entry;

    @SuppressWarnings("deprecation")
    void enter() {
        try {
            Object instance = null;

            if (!this.isStatic()) {
                instance = this.entry.getDeclaringClass().newInstance();
            }

            this.entry.invoke(instance);
        } catch (Exception e) {
            FastLogger.logStatic("Couldn't start the app, this is traditionally considered your fault. Here's the error:");
            FastLogger.logException(e);
            System.exit(1);
        }
    }

    private boolean isStatic() {
        return Modifier.isStatic(this.entry.getModifiers());
    }

}
