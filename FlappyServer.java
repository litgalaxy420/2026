import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Flappy Penguin Server
 * Serves the Flappy Penguin game on 127.0.0.1 with a random port
 * 
 * Created by: Caleb D.
 */
public class FlappyServer {
    
    // Blue Raspberry Color Scheme
    private static final String BLUE = "\u001B[38;2;0;168;232m";
    private static final String DARK_BLUE = "\u001B[38;2;0;126;151m";
    private static final String LIGHT_BLUE = "\u001B[38;2;0;195;255m";
    private static final String RASPBERRY = "\u001B[38;2;194;30;86m";
    private static final String WHITE = "\u001B[38;2;255;255;255m";
    private static final String GREEN = "\u001B[38;2;0;200;100m";
    private static final String RESET = "\u001B[0m";
    
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    
    static {
        MIME_TYPES.put(".html", "text/html");
        MIME_TYPES.put(".css", "text/css");
        MIME_TYPES.put(".js", "application/javascript");
        MIME_TYPES.put(".json", "application/json");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".gif", "image/gif");
        MIME_TYPES.put(".svg", "image/svg+xml");
        MIME_TYPES.put(".ico", "image/x-icon");
    }
    
    private static final int DEFAULT_PORT = 8080;
    
    private static int getRandomPort() {
        return DEFAULT_PORT;
    }
    
    private static String getMimeType(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = path.substring(dotIndex).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
    
    private static void printBanner(int port) {
        System.out.println(BLUE + "╔════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BLUE + "║                                                            ║" + RESET);
        System.out.println(BLUE + "║   ██████╗ ██████╗  █████╗ ██╗     ██╗     ███████╗██████╗  ║" + RESET);
        System.out.println(BLUE + "║   ██╔══██╗██╔══██╗██╔══██╗██║     ██║     ██╔════╝██╔══██╗ ║" + RESET);
        System.out.println(BLUE + "║   ██████╔╝██████╔╝███████║██║     ██║     █████╗  ██████╔╝ ║" + RESET);
        System.out.println(BLUE + "║   ██╔═══╝ ██╔══██╗██╔══██║██║     ██║     ██╔══╝  ██╔══██╗ ║" + RESET);
        System.out.println(BLUE + "║   ██║     ██║  ██║██║  ██║███████╗███████╗███████╗██║  ██║ ║" + RESET);
        System.out.println(BLUE + "║   ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝╚═╝  ╚═╝ ║" + RESET);
        System.out.println(BLUE + "║                                                            ║" + RESET);
        System.out.println(BLUE + "║              Flappy Penguin Game Server                    ║" + RESET);
        System.out.println(BLUE + "║                                                            ║" + RESET);
        System.out.println(BLUE + "║                    Created by: Caleb D.                     ║" + RESET);
        System.out.println(BLUE + "║                                                            ║" + RESET);
        System.out.println(BLUE + "╚════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        System.out.println(LIGHT_BLUE + "🚀 Server is running!" + RESET);
        System.out.println();
        System.out.println(WHITE + "📍 Address: http://127.0.0.1:" + port + RESET);
        System.out.println();
        System.out.println(WHITE + "🎮 Game URL: http://127.0.0.1:" + port + "/flappy-penguin.html" + RESET);
        System.out.println();
        System.out.println(WHITE + "💡 Press Ctrl+C to stop the server" + RESET);
        System.out.println();
    }
    
    private static String getNotFoundPage(int port) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>404 - Not Found</title>\n" +
               "    <style>\n" +
               "        body {\n" +
               "            background-color: #1a1a2e;\n" +
               "            color: " + WHITE + ";\n" +
               "            font-family: 'Arial', sans-serif;\n" +
               "            display: flex;\n" +
               "            justify-content: center;\n" +
               "            align-items: center;\n" +
               "            height: 100vh;\n" +
               "            margin: 0;\n" +
               "        }\n" +
               "        .container {\n" +
               "            text-align: center;\n" +
               "        }\n" +
               "        h1 {\n" +
               "            color: " + LIGHT_BLUE + ";\n" +
               "            font-size: 48px;\n" +
               "        }\n" +
               "        p {\n" +
               "            color: " + BLUE + ";\n" +
               "            font-size: 24px;\n" +
               "        }\n" +
               "        .back-btn {\n" +
               "            background: linear-gradient(180deg, " + BLUE + ", " + DARK_BLUE + ");\n" +
               "            color: white;\n" +
               "            padding: 15px 40px;\n" +
               "            font-size: 20px;\n" +
               "            border: 3px solid " + LIGHT_BLUE + ";\n" +
               "            border-radius: 10px;\n" +
               "            cursor: pointer;\n" +
               "            text-decoration: none;\n" +
               "            display: inline-block;\n" +
               "            margin-top: 20px;\n" +
               "        }\n" +
               "        .back-btn:hover {\n" +
               "            background: linear-gradient(180deg, " + LIGHT_BLUE + ", " + BLUE + ");\n" +
               "        }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"container\">\n" +
               "        <h1>404</h1>\n" +
               "        <p>Page Not Found</p>\n" +
               "        <a href=\"http://127.0.0.1:" + port + "/flappy-penguin.html\" class=\"back-btn\">Go to Flappy Penguin</a>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    public static void main(String[] args) {
        int port = getRandomPort();
        
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))) {
            printBanner(port);
            
            // Handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.println(RASPBERRY + "🛑 Shutting down server..." + RESET);
                System.out.println(GREEN + "✅ Server stopped successfully!" + RESET);
            }));
            
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     OutputStream out = clientSocket.getOutputStream()) {
                    
                    String requestLine = in.readLine();
                    if (requestLine == null) continue;
                    
                    // Parse request
                    String[] parts = requestLine.split(" ");
                    String method = parts[0];
                    String path = parts[1];
                    
                    // Read headers (ignore them)
                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        // Skip headers
                    }
                    
                    // Default to flappy-penguin.html
                    String filePath = path.equals("/") ? "/flappy-penguin.html" : path;
                    
                    // Remove query string
                    int queryIndex = filePath.indexOf('?');
                    if (queryIndex > 0) {
                        filePath = filePath.substring(0, queryIndex);
                    }
                    
                    // Get the directory of the class file
                    String baseDir = System.getProperty("user.dir");
                    String fullPath = baseDir + filePath.replace('/', File.separatorChar);
                    
                    File file = new File(fullPath);
                    
                    if (file.exists() && file.isFile()) {
                        // Serve the file
                        String mimeType = getMimeType(file.getName());
                        byte[] fileContent = Files.readAllBytes(file.toPath());
                        
                        String response = "HTTP/1.1 200 OK\r\n" +
                                          "Content-Type: " + mimeType + "\r\n" +
                                          "Content-Length: " + fileContent.length + "\r\n" +
                                          "Connection: close\r\n" +
                                          "\r\n";
                        
                        out.write(response.getBytes());
                        out.write(fileContent);
                    } else {
                        // Serve 404 page
                        String notFoundPage = getNotFoundPage(port);
                        byte[] pageContent = notFoundPage.getBytes();
                        
                        String response = "HTTP/1.1 404 Not Found\r\n" +
                                          "Content-Type: text/html\r\n" +
                                          "Content-Length: " + pageContent.length + "\r\n" +
                                          "Connection: close\r\n" +
                                          "\r\n";
                        
                        out.write(response.getBytes());
                        out.write(pageContent);
                    }
                    
                } catch (IOException e) {
                    System.err.println(RASPBERRY + "Error handling client: " + e.getMessage() + RESET);
                }
            }
            
        } catch (IOException e) {
            System.err.println(RASPBERRY + "Failed to start server: " + e.getMessage() + RESET);
            System.exit(1);
        }
    }
}
