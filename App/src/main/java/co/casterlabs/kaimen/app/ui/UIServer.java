package co.casterlabs.kaimen.app.ui;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

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
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class UIServer implements Closeable {
    private @Setter Function<HttpSession, HttpResponse> handler;

    private HttpServer server;
    private @Getter int port;
    private @Getter String password;

    private @Getter String address;
    private @Getter String localAddress;

    // Still unable to set the webview password in WV. :(
    private @Setter @Getter boolean ignorePassword = false;

    @SneakyThrows
    public UIServer() {
        final String webviewPassword = Webview.getPassword();
        final String baseDomain = String.format("%s.127-0-0-1.sslip.io", webviewPassword); // https://sslip.io/

        // Find a random port.
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0), 1);
            this.port = serverSocket.getLocalPort();
        }

        this.address = String.format("http://%s:%d", baseDomain, this.port);
        this.localAddress = String.format("http://127.0.0.1:%d", this.port);

        this.server = HttpServerBuilder
            .getUndertowBuilder()
            .setHostname("127.0.0.1")
            .setPort(this.port)
            .build(new HttpListener() {
                @Override
                public @Nullable HttpResponse serveSession(@NonNull String host, @NonNull HttpSession session, boolean secure) {
                    String userAgent = session.getHeader("User-Agent");

                    if (host.contains(baseDomain) || userAgent.contains(webviewPassword) || ignorePassword) {
                        return handler.apply(session);
                    }

                    return null;
                }

                @Override
                public @Nullable WebsocketListener serveWebsocketSession(@NonNull String host, @NonNull WebsocketSession session, boolean secure) {
                    return null;
                }
            });
    }

    public UIServer start() throws IOException {
        this.server.start();
        return this;
    }

    @Override
    public void close() throws IOException {
        this.server.stop();
    }

}
