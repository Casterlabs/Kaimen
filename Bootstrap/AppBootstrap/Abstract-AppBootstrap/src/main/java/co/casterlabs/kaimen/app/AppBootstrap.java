package co.casterlabs.kaimen.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.reflections8.Reflections;
import org.reflections8.scanners.MethodAnnotationsScanner;

import co.casterlabs.commons.async.queue.ThreadQueue;
import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

public class AppBootstrap {

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true); // Enable assertions.

        // Go ahead and get the access warning out of the way
        try {
            AccessHelper.makeAccessible((Method) null);
        } catch (Throwable ignored) {}
        System.err.println("\n------------ ^ Ignore that warning ^ ------------\n\n");

        App instance = null;
        switch (co.casterlabs.commons.platform.Platform.osDistribution) {
            case LINUX:
                instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.linux.LinuxBootstrap").newInstance();
                break;

            case MACOSX:
                instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.macos.MacOSBootstrap").newInstance();
                break;

            case WINDOWS_NT:
                instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.windows.WindowsBootstrap").newInstance();
                break;

            default:
                throw new IllegalStateException("Failed to find bootstrap");
        }

        // Init the mainThread.
        boolean useDefaultMainThreadImpl = instance.getMainThreadImpl() == null;
        ThreadQueue mainThread;

        if (useDefaultMainThreadImpl) {
            mainThread = new ThreadQueue();
        } else {
            mainThread = new ThreadQueue(instance.getMainThreadImpl());
        }

        // Try to give the ThreadQueue to the Webview package.
        try {
            Class<?> clazz = Class.forName("co.casterlabs.kaimen.webview.Webview");
            ReflectionLib.setStaticValue(clazz, "mainThread", mainThread);
        } catch (Exception ignored) {}

        // Init the framework
        App.init(args, instance, mainThread);

        // Enter into the app
        AppEntryPoint entryPoint = findEntryPoint();

        if (entryPoint.entryAnnotation.startOnMainThread()) {
            mainThread.submitTask(entryPoint::enter);
        } else {
            entryPoint.enter();
        }
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
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getCause();
            }

            FastLogger.logStatic("Couldn't start the app, this is traditionally considered your fault. Here's the error:");
            FastLogger.logException(t);
            System.exit(1);
        }
    }

    private boolean isStatic() {
        return Modifier.isStatic(this.entry.getModifiers());
    }

}
