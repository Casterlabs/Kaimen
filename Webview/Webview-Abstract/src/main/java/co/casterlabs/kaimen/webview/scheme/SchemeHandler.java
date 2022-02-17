package co.casterlabs.kaimen.webview.scheme;

import co.casterlabs.kaimen.webview.scheme.http.HttpRequest;
import co.casterlabs.kaimen.webview.scheme.http.HttpResponse;
import lombok.NonNull;

public interface SchemeHandler {

    public @NonNull HttpResponse onRequest(@NonNull HttpRequest request);

}
