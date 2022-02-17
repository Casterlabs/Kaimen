package co.casterlabs.kaimen.app.ui;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.functional.ConsumingProducer;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.rakurai.io.http.HttpResponse;
import co.casterlabs.rakurai.io.http.HttpSession;
import co.casterlabs.rakurai.io.http.server.HttpListener;
import co.casterlabs.rakurai.io.http.server.HttpServer;
import co.casterlabs.rakurai.io.http.server.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.websocket.WebsocketListener;
import co.casterlabs.rakurai.io.http.websocket.WebsocketSession;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public class UIServer implements Closeable {

    private @Setter ConsumingProducer<HttpSession, HttpResponse> handler;

    private HttpServer server;
    private @Getter int port;

    @SneakyThrows
    public UIServer() {
        String webviewToken = ReflectionLib.getStaticValue(Webview.class, "webviewToken");

        // Find a random port.
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0), 1);
            this.port = serverSocket.getLocalPort();
        }

        this.server = HttpServerBuilder
            .getUndertowBuilder()
            .setHostname("127.0.0.1")
            .setPort(this.port)
            .build(new HttpListener() {
                @Override
                public @Nullable HttpResponse serveSession(@NonNull String host, @NonNull HttpSession session, boolean secure) {
                    if (session.getHeader("User-Agent").contains(webviewToken)) {
                        try {
                            return handler.produce(session);
                        } catch (InterruptedException e) {}
                    }

                    return null;
                }

                @Override
                public @Nullable WebsocketListener serveWebsocketSession(@NonNull String host, @NonNull WebsocketSession session, boolean secure) {
                    return null;
                }
            });
    }

    public String getAddress() {
        return String.format("http://127.0.0.1:%d", this.port);
    }

    public void start() throws IOException {
        this.server.start();
    }

    @Override
    public void close() throws IOException {
        this.server.stop();
    }

}
