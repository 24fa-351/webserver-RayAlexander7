import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class SimpleWebServer {
    private static int requestCount = 0;  // Count of total requests
    private static int bytesReceived = 0; // Count of received bytes
    private static int bytesSent = 0; // Count of sent bytes

    public static void main(String[] args) throws IOException {
        int port = 80;  // Default port
        if (args.length > 0 && args[0].equals("-p") && args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        // Create server socket
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        // Create thread pool to handle multiple requests concurrently
        ExecutorService executor = Executors.newFixedThreadPool(10);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(new ClientHandler(clientSocket));
        }
    }

    // ClientHandler class to handle each request
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String requestLine = in.readLine();
                if (requestLine == null) {
                    return;
                }

                // Process the request
                String[] tokens = requestLine.split(" ");
                String method = tokens[0];
                String path = tokens[1];

                // Increase the count of received requests and bytes
                requestCount++;
                bytesReceived += requestLine.length();

                // Serve different paths
                if (path.startsWith("/static")) {
                    serveStaticFile(path, out);
                } else if (path.equals("/stats")) {
                    serveStats(out);
                } else if (path.startsWith("/calc")) {
                    serveCalculation(path, out);
                } else {
                    // Return 404 for unknown paths
                    out.println("HTTP/1.1 404 Not Found");
                    out.println("Content-Type: text/html");
                    out.println();
                    out.println("<html><body><h1>404 Not Found</h1></body></html>");
                }

                // Increase sent bytes
                bytesSent += out.toString().length();
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void serveStaticFile(String path, PrintWriter out) throws IOException {
            File file = new File("static" + path.substring(7));  // Removing "/static" from the path
            if (file.exists()) {
                String extension = getFileExtension(file);
                String contentType = getContentType(extension);

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType);
                out.println();
                bytesSent += file.length();

                // Serve file
                try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(new String(buffer, 0, bytesRead));
                    }
                }
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<html><body><h1>404 Not Found</h1></body></html>");
            }
        }

        private void serveStats(PrintWriter out) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<html><body>");
            out.println("<h1>Server Stats</h1>");
            out.println("<p>Requests received: " + requestCount + "</p>");
            out.println("<p>Bytes received: " + bytesReceived + "</p>");
            out.println("<p>Bytes sent: " + bytesSent + "</p>");
            out.println("</body></html>");
        }

        private void serveCalculation(String path, PrintWriter out) {
            try {
                Pattern pattern = Pattern.compile(".*[?&]a=(\\d+)&b=(\\d+).*");
                Matcher matcher = pattern.matcher(path);
                if (matcher.matches()) {
                    int a = Integer.parseInt(matcher.group(1));
                    int b = Integer.parseInt(matcher.group(2));
                    int sum = a + b;
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println();
                    out.println("<html><body>");
                    out.println("<h1>Sum</h1>");
                    out.println("<p>The sum of " + a + " and " + b + " is " + sum + "</p>");
                    out.println("</body></html>");
                } else {
                    out.println("HTTP/1.1 400 Bad Request");
                    out.println("Content-Type: text/html");
                    out.println();
                    out.println("<html><body><h1>400 Bad Request</h1></body></html>");
                }
            } catch (NumberFormatException e) {
                out.println("HTTP/1.1 400 Bad Request");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<html><body><h1>400 Bad Request</h1></body></html>");
            }
        }

        private String getFileExtension(File file) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                return name.substring(dotIndex + 1);
            }
            return "";
        }

        private String getContentType(String extension) {
            switch (extension) {
                case "html":
                case "htm":
                    return "text/html";
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                case "css":
                    return "text/css";
                case "js":
                    return "application/javascript";
                default:
                    return "application/octet-stream";
            }
        }
    }
}
