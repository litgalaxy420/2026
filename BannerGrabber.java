import java.io.*;
import java.net.*;
import java.util.*;

public class BannerGrabber {
    private static final int TIMEOUT_MS = 3000;
    private static final int MAX_BYTES = 1024;

    public static void main(String[] args) {
        System.out.println("\033[92m"); // Green text
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║         BANNER GRABBER by Caleb D          ║");
        System.out.println("║   TCP Port Service Banner Identification   ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println("\033[0m");

        if (args.length < 2) {
            printUsage();
            return;
        }

        String targetIp = args[0];
        String[] portStrings = args[1].split(",");

        System.out.println("\033[92m[\033[0m+\033[92m] Target: " + targetIp);
        System.out.println("[\033[0m+\033[92m] Scanning " + portStrings.length + " port(s)...\033[0m");
        System.out.println();

        for (String portStr : portStrings) {
            try {
                int port = Integer.parseInt(portStr.trim());
                grabBanner(targetIp, port);
            } catch (NumberFormatException e) {
                System.out.println("\033[91m[!] Invalid port number: " + portStr + "\033[0m");
            }
        }

        System.out.println();
        System.out.println("\033[92m[\033[0m+\033[92m] Scan complete.\033[0m");
    }

    private static void grabBanner(String ip, int port) {
        System.out.print("\033[92m[\033[0m?\033[92m] Port " + port + ": \033[0m");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[MAX_BYTES];
            int bytesRead = 0;

            // Read available bytes up to MAX_BYTES
            try {
                bytesRead = in.read(buffer);
            } catch (SocketTimeoutException e) {
                // Partial data received
            }

            if (bytesRead > 0) {
                String banner = new String(buffer, 0, bytesRead).trim();
                // Remove non-printable characters
                banner = banner.replaceAll("[\\x00-\\x1F\\x7F]", "");
                System.out.println(banner);
            } else if (bytesRead == 0) {
                System.out.println("\033[93m[Connection open, no data received]\033[0m");
            }

        } catch (IOException e) {
            System.out.println("\033[91m[Error: " + e.getMessage() + "]\033[0m");
        }
    }

    private static void printUsage() {
        System.out.println("\033[93m");
        System.out.println("Usage: java BannerGrabber <IP> <ports>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  <IP>     - Target IP address");
        System.out.println("  <ports>  - Comma-separated list of ports (e.g., 22,80,443)");
        System.out.println();
        System.out.println("Example: java BannerGrabber 192.168.1.1 22,80,443,3306");
        System.out.println("\033[0m");
    }
}
