package co.casterlabs.kaimen.webview;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.streams.StreamUtil;

public class WebviewFileUtil {
    private static boolean isDev;

    public static String loadResource(String path) throws IOException {
        InputStream in;

        if (isDev) {
            in = new FileInputStream(new File("../../Bootstrap/src/main/resources/", path));
        } else {
            in = WebviewFileUtil.class.getClassLoader().getResourceAsStream(path);
        }

        return StreamUtil.toString(in, StandardCharsets.UTF_8);
    }

    public static String loadResourceFromBuildProject(String path, String project) throws IOException {
        InputStream in;

        if (isDev) {
            in = new FileInputStream(new File(String.format("../../Webview/%s/src/main/resources/%s", project, path)));
        } else {
            in = WebviewFileUtil.class.getClassLoader().getResourceAsStream(path);
        }

        return StreamUtil.toString(in, StandardCharsets.UTF_8);
    }

    public static byte[] loadResourceBytes(String path) throws IOException {
        InputStream in;

        if (isDev) {
            in = new FileInputStream(new File("../../Bootstrap/src/main/resources/", path));
        } else {
            in = WebviewFileUtil.class.getClassLoader().getResourceAsStream(path);
        }

        return StreamUtil.toBytes(in);
    }

    public static URL loadResourceAsUrl(String path) throws IOException {
        if (isDev) {
            return new File("../../Bootstrap/src/main/resources/", path).toURI().toURL();
        } else {
            return WebviewFileUtil.class.getClassLoader().getResource(path);
        }
    }

}
