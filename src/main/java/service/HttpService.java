package service;

import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.jsoup.Jsoup.parse;

public class HttpService {
    private static final int httpPort = 80;
    private static byte[] pattern = new byte[]{13, 10, 13, 10};
    private static final Path downloadPath = Paths.get("src/main/resources");

    private final String hostName;
    private final String ipAddress;

    public HttpService(String ipAddress, String hostName) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
    }

    public void displayHttpInfo() throws IOException {
        List<String> imageSource;
        Document document;
        String line;

        try (Socket socket = new Socket(ipAddress, httpPort)) {

            send("/", socket.getOutputStream());

            try (InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
                 BufferedReader bufferedReader = new BufferedReader(streamReader)) {

                System.out.println("***** HTTP Header *****");
                do {
                    line = bufferedReader.readLine();
                    System.out.println(line);
                } while (!line.equals(""));

                System.out.println("***** HTTP Body *****");
                document = parse(bufferedReader.lines().collect(Collectors.joining()), "utf-8");
                System.out.println(document.html());

                imageSource = document.getElementsByTag("img").eachAttr("src");
            }
        }

        downloadImages(imageSource);
    }

    private void send(String path, OutputStream stream) {
        PrintWriter printWriter = new PrintWriter(stream);

        printWriter.println("GET " + path + " HTTP/1.1");
        printWriter.println("Host: " + hostName);
        printWriter.println("Connection: close");
        printWriter.println("");

        printWriter.flush();
    }

    private void downloadImages(List<String> imagesSource) {
        System.out.println("***** Images *****");

        imagesSource.forEach(source -> {
            byte[] bytes;
            int position;
            String name;
            Path path;

            System.out.println(source);

            try (Socket socket = new Socket(ipAddress, httpPort)) {
                send(source, socket.getOutputStream());

                InputStream inputStream = socket.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                bytes = buffer.toByteArray();

                position = indexOf(bytes, pattern);
                position += pattern.length;

                name = source.substring(source.lastIndexOf('/') + 1);
                path = downloadPath.resolve(name);

                Files.createFile(path);
                try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                    outputStream.write(Arrays.copyOfRange(bytes, position, bytes.length));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private int indexOf(byte[] bigger, byte[] smaller) {
        for (int i = 0; i < bigger.length - smaller.length + 1; ++i) {
            boolean found = true;

            for (int j = 0; j < smaller.length; ++j) {
                if (bigger[i + j] != smaller[j]) {
                    found = false;
                    break;
                }
            }

            if (found) return i;
        }

        return -1;
    }
}
