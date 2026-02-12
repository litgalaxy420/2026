import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.net.http.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ENV File Finder - Searches for exposed .env files and sensitive environment files
 * Tests websites and subdomains for vulnerable .env file exposure via directory traversal,
 * common paths, and misconfigured web servers
 * 
 * Credit: Caleb Drawhorn
 */
public class ENVFileFinder {
    private static final int TIMEOUT_SECONDS = 10;
    private static final int THREAD_POOL_SIZE = 10;
    private HttpClient httpClient;
    
    private List<String> foundFiles = new ArrayList<>();
    private List<String> foundDomains = new ArrayList<>();
    private int filesChecked = 0;
    private int successfulResponses = 0;

    // Common .env file paths to check
    private static final String[] ENV_PATHS = {
            "/.env",
            "/.env.local",
            "/.env.example",
            "/.env.backup",
            "/.env.old",
            "/.env.prod",
            "/.env.development",
            "/.env.staging",
            "/.env.test",
            "/config/.env",
            "/config/env",
            "/.env.php",
            "/.git/.env",
            "/app/.env",
            "/src/.env",
            "/.env~",
            "/.env.bak",
            "/.env.dist",
            "/env.php",
            "/env.json",
            "/config.env",
            "/.env.js",
            "/.env.ts",
            "/settings/.env"
    };

    // Common subdomains that might expose logs/env files
    private static final String[] COMMON_SUBDOMAINS = {
            "api",
            "admin",
            "app",
            "staging",
            "dev",
            "development",
            "test",
            "logs",
            "backup",
            "config",
            "env",
            "cms",
            "wp-admin",
            "mail",
            "smtp",
            "ftp",
            "files",
            "downloads",
            "uploads",
            "cdn",
            "static",
            "assets",
            "www",
            "old",
            "beta",
            "internal",
            "private",
            "secure"
    };

    // Sensitive env variable patterns
    private static final String[] SENSITIVE_PATTERNS = {
            "(?i)password\\s*=",
            "(?i)api_?key\\s*=",
            "(?i)secret\\s*=",
            "(?i)token\\s*=",
            "(?i)db_?pass\\s*=",
            "(?i)aws_?secret\\s*=",
            "(?i)private_?key\\s*=",
            "(?i)oauth\\s*=",
            "(?i)stripe\\s*=",
            "(?i)mongodb\\s*=",
            "(?i)mysql\\s*=",
            "(?i)postgres\\s*="
    };

    public ENVFileFinder() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Search for exposed .env files on a domain
     */
    public void searchDomain(String domain, boolean checkSubdomains, boolean verbose) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  ENV FILE FINDER & EXPOSURE SCANNER     ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.println("[*] Target Domain: " + domain);
        System.out.println("[*] Checking Subdomains: " + checkSubdomains);
        System.out.println("[*] Verbose Mode: " + verbose);
        System.out.println("[*] Starting scan...\n");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        // Check main domain
        checkDomainForEnvFiles(executor, futures, domain, verbose);

        // Check common subdomains
        if (checkSubdomains) {
            System.out.println("[*] Enumerating subdomains...");
            for (String subdomain : COMMON_SUBDOMAINS) {
                String fullDomain = subdomain + "." + domain;
                checkDomainForEnvFiles(executor, futures, fullDomain, verbose);
            }
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                if (verbose) {
                    System.err.println("[-] Error: " + e.getMessage());
                }
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("[-] Executor interrupted");
        }

        printResults();
    }

    /**
     * Check a specific domain for .env files
     */
    private void checkDomainForEnvFiles(ExecutorService executor, List<Future<?>> futures,
                                       String domain, boolean verbose) {
        String baseUrl = "http://" + domain;
        
        for (String envPath : ENV_PATHS) {
            futures.add(executor.submit(() -> {
                testUrl(baseUrl + envPath, domain, verbose);
            }));
        }

        // Also check HTTPS
        String baseUrlHttps = "https://" + domain;
        for (String envPath : ENV_PATHS) {
            futures.add(executor.submit(() -> {
                testUrl(baseUrlHttps + envPath, domain, verbose);
            }));
        }

        // Check for directory listing that might expose .env
        futures.add(executor.submit(() -> {
            testDirectoryListing(baseUrl, domain, verbose);
            testDirectoryListing(baseUrlHttps, domain, verbose);
        }));
    }

    /**
     * Test a single URL for .env file
     */
    private void testUrl(String url, String domain, boolean verbose) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            synchronized (this) {
                filesChecked++;
                
                if (response.statusCode() == 200) {
                    successfulResponses++;
                    String content = response.body();

                    // Check for suspicious content
                    if (isSuspiciousContent(content)) {
                        System.out.println("[!!!] FOUND EXPOSED FILE: " + url);
                        foundFiles.add(url);
                        
                        // Check for sensitive data
                        int sensitiveCount = countSensitivePatterns(content);
                        if (sensitiveCount > 0) {
                            System.out.println("     [!] Contains " + sensitiveCount + " sensitive pattern(s)");
                        }
                        
                        if (verbose) {
                            System.out.println("     Preview: " + content.substring(0, Math.min(100, content.length())));
                        }
                    }
                } else if (response.statusCode() != 404 && response.statusCode() != 403) {
                    if (verbose && response.statusCode() != 301 && response.statusCode() != 302) {
                        System.out.println("[?] " + url + " - Status: " + response.statusCode());
                    }
                }
            }

        } catch (ConnectException e) {
            if (verbose) {
                System.out.println("[-] Connection failed: " + domain);
            }
        } catch (URISyntaxException e) {
            System.err.println("[-] Invalid URI: " + url);
        } catch (IOException | InterruptedException e) {
            // Silently skip timeouts and connection errors
        }
    }

    /**
     * Test if directory listing is enabled
     */
    private void testDirectoryListing(String baseUrl, String domain, boolean verbose) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(baseUrl + "/"))
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String content = response.body();
                // Check for directory listing patterns
                if (content.contains(".env") || content.contains("Index of") || 
                    content.matches("(?s).*<a href=.*")) {
                    System.out.println("[+] Directory listing detected on " + baseUrl);
                    if (content.contains(".env")) {
                        System.out.println("[!!!] .env file reference found in directory!");
                        foundDomains.add(domain);
                    }
                }
            }
        } catch (Exception e) {
            // Skip
        }
    }

    /**
     * Check if content looks like an .env file
     */
    private boolean isSuspiciousContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // Check for .env file patterns
        int patternMatches = 0;
        
        if (content.contains("=") && !content.contains("html")) {
            patternMatches++;
        }
        if (content.matches("(?i).*DATABASE.*=.*")) {
            patternMatches++;
        }
        if (content.matches("(?i).*KEY.*=.*")) {
            patternMatches++;
        }
        if (content.matches("(?i).*SECRET.*=.*")) {
            patternMatches++;
        }
        if (content.matches("(?i).*PASSWORD.*=.*")) {
            patternMatches++;
        }
        if (content.matches("(?i).*TOKEN.*=.*")) {
            patternMatches++;
        }

        // If content has multiple suspicious patterns, likely an env file
        return patternMatches >= 2 || countSensitivePatterns(content) > 0;
    }

    /**
     * Count matching sensitive patterns in content
     */
    private int countSensitivePatterns(String content) {
        int count = 0;
        for (String pattern : SENSITIVE_PATTERNS) {
            if (Pattern.compile(pattern).matcher(content).find()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Print results summary
     */
    private void printResults() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SCAN RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Files Checked: " + filesChecked);
        System.out.println("Successful Responses (200): " + successfulResponses);
        System.out.println("Exposed Files Found: " + foundFiles.size());
        System.out.println("Domains with Exposed Content: " + foundDomains.size());

        if (!foundFiles.isEmpty()) {
            System.out.println("\n[!!!] EXPOSED .ENV FILES DETECTED:");
            for (String file : foundFiles) {
                System.out.println("    -> " + file);
            }
        }

        if (!foundDomains.isEmpty()) {
            System.out.println("\n[!!!] DOMAINS WITH EXPOSED REFERENCES:");
            for (String domain : foundDomains) {
                System.out.println("    -> " + domain);
            }
        }

        if (foundFiles.isEmpty() && foundDomains.isEmpty()) {
            System.out.println("\n[+] No exposed .env files detected");
        }

        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Interactive mode
     */
    public static void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        ENVFileFinder finder = new ENVFileFinder();

        while (true) {
            System.out.println("\nOptions:");
            System.out.println("1. Scan domain for exposed .env files");
            System.out.println("2. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Enter domain (e.g., example.com): ");
                String domain = scanner.nextLine().trim();

                System.out.print("Check subdomains? (y/n) [y]: ");
                String subdomainChoice = scanner.nextLine().trim().toLowerCase();
                boolean checkSubdomains = !subdomainChoice.equals("n");

                System.out.print("Verbose output? (y/n) [n]: ");
                String verboseChoice = scanner.nextLine().trim().toLowerCase();
                boolean verbose = verboseChoice.equals("y");

                finder.searchDomain(domain, checkSubdomains, verbose);

            } else if (choice.equals("2")) {
                System.out.println("[*] Exiting...");
                break;
            } else {
                System.out.println("[-] Invalid option");
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            interactiveMode();
        } else if (args.length >= 1) {
            // Command-line mode
            String domain = args[0];
            boolean checkSubdomains = args.length > 1 ? Boolean.parseBoolean(args[1]) : true;
            boolean verbose = args.length > 2 ? Boolean.parseBoolean(args[2]) : false;

            ENVFileFinder finder = new ENVFileFinder();
            finder.searchDomain(domain, checkSubdomains, verbose);
        } else {
            System.out.println("Usage: java ENVFileFinder <domain> [check_subdomains] [verbose]");
            System.out.println("\nExamples:");
            System.out.println("  java ENVFileFinder example.com");
            System.out.println("  java ENVFileFinder example.com true false");
            System.out.println("  java ENVFileFinder example.com true true (verbose)\n");
        }
    }
}
