package co.casterlabs.kaimen.webview;

import org.jetbrains.annotations.Nullable;

public interface WebviewLifeCycleListener {

    default void onBrowserPreLoad() {}

    default void onBrowserOpen() {}

    default void onBrowserClose() {}

    default void onMinimize() {}

    default void onOpenRequested() {}

    default void onCloseRequested() {
        System.exit(0);
    }

    default void onPageTitleChange(@Nullable String newTitle) {}

    default void onNavigate(String url) {}

}
