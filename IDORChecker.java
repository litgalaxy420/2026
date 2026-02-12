import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * IDOR (Insecure Direct Object References) Vulnerability Checker
 * Tests website endpoints for potential IDOR vulnerabilities by attempting
 * to access resources with different ID values
 */
public class IDORChecker {
    // ANSI Colors
    private static final String PINK = "\u001B[35m";
    private static final String BRIGHT_PINK = "\u001B[95m";
    private static final String RESET = "\u001B[0m";
    private static final int TIMEOUT_SECONDS = 10;
    private static final int THREAD_POOL_SIZE = 5;
    private HttpClient httpClient;
    private int successCount = 0;
    private int vulnerableCount = 0;
    private List<String> vulnerableEndpoints = new ArrayList<>();
    private static final String PINK_RESET = BRIGHT_PINK + "%-60s" + RESET;

    public IDORChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Check an endpoint for IDOR vulnerabilities
     * @param baseUrl Base URL with placeholder (e.g., http://target.com/api/users/ID)
     * @param idRange Range of IDs to test (e.g., 1-100)
     * @param authHeader Optional authorization header (can be null)
     * @param expectedStatus Expected HTTP status code for authorized access
     */
    public void checkEndpoint(String baseUrl, String idRange, String authHeader, int expectedStatus) {
        System.out.println(BRIGHT_PINK + "\n[*] Testing endpoint: " + baseUrl + RESET);
        System.out.println(BRIGHT_PINK + "[*] ID Range: " + idRange + RESET);
        System.out.println(BRIGHT_PINK + "[*] Expected Status: " + expectedStatus + RESET);

        int[] ids = parseIdRange(idRange);
        if (ids.length == 0) {
            System.err.println("[-] Invalid ID range format");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        for (int id : ids) {
            futures.add(executor.submit(() -> testId(baseUrl, id, authHeader, expectedStatus)));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("[-] Error during execution: " + e.getMessage());
            }
        }

        executor.shutdown();
        printResults();
    }

    /**
     * Test a single ID value
     */
    private void testId(String baseUrl, int id, String authHeader, int expectedStatus) {
        try {
            String url = baseUrl.replace("ID", String.valueOf(id));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET();

            if (authHeader != null && !authHeader.isEmpty()) {
                requestBuilder.header("Authorization", authHeader);
            }

            // Add common headers to appear more like a browser
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            requestBuilder.header("Accept", "application/json");

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            synchronized (this) {
                successCount++;

                // Check if we got an unauthorized response when accessing another user's resource
                if (response.statusCode() == expectedStatus) {
                    System.out.println(BRIGHT_PINK + "[+] ID " + id + " - Status: " + response.statusCode() +
                            " [POTENTIAL IDOR - Accessible]" + RESET);
                    vulnerableCount++;
                    vulnerableEndpoints.add(url + " (Status: " + response.statusCode() + ")");
                } else if (response.statusCode() == 403 || response.statusCode() == 404) {
                    System.out.println(PINK + "[-] ID " + id + " - Status: " + response.statusCode() +
                            " [Protected]" + RESET);
                } else {
                    System.out.println(PINK + "[?] ID " + id + " - Status: " + response.statusCode() + RESET);
                    if (response.statusCode() == 200 && response.statusCode() != expectedStatus) {
                        vulnerableCount++;
                        vulnerableEndpoints.add(url + " (Unexpected access with status: " +
                                response.statusCode() + ")");
                    }
                }
            }

        } catch (URISyntaxException e) {
            System.err.println("[-] Invalid URI: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("[-] Error testing ID " + id + ": " + e.getMessage());
        }
    }

    /**
     * Parse ID range (e.g., "1-100", "1,5,10,20", "1-10,50-60")
     */
    private int[] parseIdRange(String idRange) {
        Set<Integer> ids = new TreeSet<>();

        String[] parts = idRange.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = start; i <= end; i++) {
                            ids.add(i);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[-] Invalid range format: " + part);
                    }
                }
            } else {
                try {
                    ids.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    System.err.println("[-] Invalid ID: " + part);
                }
            }
        }

        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Print test results
     */
    private void printResults() {
        System.out.println(BRIGHT_PINK + "\n" + "=".repeat(60));
        System.out.println("IDOR CHECK RESULTS");
        System.out.println("=".repeat(60) + RESET);
        System.out.println(BRIGHT_PINK + "Total IDs Tested: " + successCount + RESET);
        System.out.println(BRIGHT_PINK + "Vulnerable Endpoints Found: " + vulnerableCount + RESET);

        if (!vulnerableEndpoints.isEmpty()) {
            System.out.println(BRIGHT_PINK + "\n[!] VULNERABLE ENDPOINTS:" + RESET);
            for (String endpoint : vulnerableEndpoints) {
                System.out.println(BRIGHT_PINK + "    -> " + endpoint + RESET);
            }
        } else {
            System.out.println(BRIGHT_PINK + "\n[+] No IDOR vulnerabilities detected" + RESET);
        }
        System.out.println(BRIGHT_PINK + "=".repeat(60) + RESET + "\n");
    }

    /**
     * Interactive mode for easier testing
     */
    public static void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        IDORChecker checker = new IDORChecker();

        System.out.println(BRIGHT_PINK + "\n╔════════════════════════════════════════╗");
        System.out.println("║  IDOR VULNERABILITY CHECKER             ║");
        System.out.println("╚════════════════════════════════════════╝\n" + RESET);

        while (true) {
            System.out.println(BRIGHT_PINK + "Options:" + RESET);
            System.out.println(BRIGHT_PINK + "1. Test endpoint" + RESET);
            System.out.println(BRIGHT_PINK + "2. Exit" + RESET);
            System.out.print(BRIGHT_PINK + "Select option: " + RESET);

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Enter URL with ID placeholder (e.g., http://api.target.com/users/ID): ");
                String url = scanner.nextLine().trim();

                if (!url.contains("ID")) {
                    System.out.println(BRIGHT_PINK + "[-] URL must contain 'ID' placeholder" + RESET);
                    continue;
                }

                System.out.print(BRIGHT_PINK + "Enter ID range (e.g., 1-100 or 1,5,10,50): " + RESET);
                String idRange = scanner.nextLine().trim();

                System.out.print(BRIGHT_PINK + "Enter authorization header (leave blank if none): " + RESET);
                String authHeader = scanner.nextLine().trim();

                System.out.print(BRIGHT_PINK + "Enter expected HTTP status code for authorized access [200]: " + RESET);
                String statusInput = scanner.nextLine().trim();
                int expectedStatus = statusInput.isEmpty() ? 200 : Integer.parseInt(statusInput);

                checker.checkEndpoint(url, idRange, authHeader.isEmpty() ? null : authHeader,
                        expectedStatus);

            } else if (choice.equals("2")) {
                System.out.println(BRIGHT_PINK + "[*] Exiting..." + RESET);
                break;
            } else {
                System.out.println(BRIGHT_PINK + "[-] Invalid option" + RESET);
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            interactiveMode();
        } else if (args.length >= 3) {
            // Command-line mode
            String baseUrl = args[0];
            String idRange = args[1];
            String authHeader = args.length > 2 ? args[2] : null;
            int expectedStatus = args.length > 3 ? Integer.parseInt(args[3]) : 200;

            IDORChecker checker = new IDORChecker();
            checker.checkEndpoint(baseUrl, idRange, authHeader, expectedStatus);
        } else {
            System.out.println(BRIGHT_PINK + "Usage: java IDORChecker <url> <id_range> [auth_header] [expected_status]");
            System.out.println("\nExamples:");
            System.out.println("  java IDORChecker \"http://api.target.com/users/ID\" \"1-50\"");
            System.out.println("  java IDORChecker \"http://api.target.com/users/ID\" \"1-100\" \"Bearer token123\"");
            System.out.println("  java IDORChecker (interactive mode)\n" + RESET);
        }
    }
}
