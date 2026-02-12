/**
 * HuntThemDown - ZoomEye API Vulnerability Scanner
 * A multithreaded tool to find vulnerable and obsolete devices/services
 * 
 * Created by: Caleb
 * Version: 1.0
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class HuntThemDown {
    private static final String VERSION = "1.0";
    private static final String AUTHOR = "Caleb";
    private static final String TOOL_NAME = "HuntThemDown";
    
    // Watermelon color scheme
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String DARK_GREEN = "\u001B[38;5;28m";
    private static final String PINK = "\u001B[38;5;205m";
    private static final String DARK_PINK = "\u001B[38;5;198m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";
    
    private static final int MAX_THREADS = 20;
    private static final int RATE_LIMIT_DELAY = 100; // ms between requests
    
    private String apiKey;
    private ExecutorService executor;
    private List<ScanResult> results;
    private int totalScanned;
    private int vulnerabilitiesFound;
    
    public HuntThemDown(String apiKey) {
        this.apiKey = apiKey;
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
        this.results = new ArrayList<>();
        this.totalScanned = 0;
        this.vulnerabilitiesFound = 0;
    }
    
    public static void main(String[] args) {
        printBanner();
        
        if (args.length < 1) {
            printUsage();
            return;
        }
        
        String apiKey = args[0];
        String target = args.length > 1 ? args[1] : "";
        
        HuntThemDown scanner = new HuntThemDown(apiKey);
        scanner.run(target);
    }
    
    private static void printBanner() {
        System.out.println(DARK_GREEN + "╔═══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + "  ██████╗  █████╗ ███╗   ███╗███████╗    " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + " ██╔════╝ ██╔══██╗████╗ ████║██╔════╝    " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + " ██║  ███╗███████║██╔████╔██║█████╗      " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + " ██║   ██║██╔══██║██║╚██╔╝██║██╔══╝      " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + " ╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗    " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + DARK_PINK + "  ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝    " + DARK_GREEN + "  ║" + RESET);
        System.out.println(DARK_GREEN + "║" + WHITE + "                    " + BOLD + "ZoomEye Vulnerability Scanner" + RESET + WHITE + "                     " + DARK_GREEN + "║" + RESET);
        System.out.println(DARK_GREEN + "║" + PINK + "                       Created by: " + AUTHOR + RESET + PINK + "                            " + DARK_GREEN + "║" + RESET);
        System.out.println(DARK_GREEN + "║" + CYAN + "                       Version: " + VERSION + RESET + CYAN + "                              " + DARK_GREEN + "║" + RESET);
        System.out.println(DARK_GREEN + "╚═══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }
    
    private static void printUsage() {
        System.out.println(GREEN + "Usage:" + RESET);
        System.out.println(GREEN + "  java HuntThemDown <API_KEY> [TARGET]" + RESET);
        System.out.println();
        System.out.println(GREEN + "Arguments:" + RESET);
        System.out.println(GREEN + "  API_KEY  - Your ZoomEye API key" + RESET);
        System.out.println(GREEN + "  TARGET   - Target device/service to search for (optional)" + RESET);
        System.out.println();
        System.out.println(GREEN + "Examples:" + RESET);
        System.out.println(GREEN + "  java HuntThemDown YOUR_API_KEY nginx" + RESET);
        System.out.println(GREEN + "  java HuntThemDown YOUR_API_KEY \"apache 2.2\"" + RESET);
        System.out.println(GREEN + "  java HuntThemDown YOUR_API_KEY \"cisco ios\"" + RESET);
    }
    
    public void run(String target) {
        System.out.println(GREEN + "[*] Starting HuntThemDown Scanner..." + RESET);
        System.out.println(GREEN + "[*] Target: " + (target.isEmpty() ? "All targets" : target) + RESET);
        System.out.println(GREEN + "[*] Threads: " + MAX_THREADS + RESET);
        System.out.println();
        
        try {
            // Define search queries for vulnerable/obsolete services
            List<String> searchQueries = createSearchQueries(target);
            
            // Submit scan tasks to thread pool
            List<Future<ScanResult>> futures = new ArrayList<>();
            
            for (String query : searchQueries) {
                ScanTask task = new ScanTask(query);
                futures.add(executor.submit(task));
            }
            
            // Collect results
            for (Future<ScanResult> future : futures) {
                try {
                    ScanResult result = future.get(30, TimeUnit.SECONDS);
                    if (result != null) {
                        results.add(result);
                        totalScanned += result.devicesFound;
                        vulnerabilitiesFound += result.vulnerabilitiesFound;
                    }
                } catch (Exception e) {
                    System.err.println(RED + "[-] Error collecting result: " + e.getMessage() + RESET);
                }
            }
            
            executor.shutdown();
            
            // Print final report
            printReport();
            
        } catch (Exception e) {
            System.err.println(RED + "[-] Scanner error: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }
    
    private List<String> createSearchQueries(String target) {
        List<String> queries = new ArrayList<>();
        
        if (!target.isEmpty()) {
            queries.add(target);
        } else {
            // Default vulnerable/obsolete service searches
            queries.add("nginx 1.4");
            queries.add("apache 2.2");
            queries.add("openssh 5.");
            queries.add("cisco ios");
            queries.add("windows server 2008");
            queries.add("ubuntu 12.04");
            queries.add("php 5.");
            queries.add("mysql 5.");
            queries.add("postgresql 9.");
            queries.add("vsftpd 2.");
            queries.add("proftpd 1.");
            queries.add("tomcat 6.");
            queries.add("jboss 5.");
            queries.add("iis 6.0");
            queries.add("oracle 10g");
            queries.add("mongodb 2.");
            queries.add("redis 2.");
            queries.add("elasticsearch 1.");
            queries.add("wordpress 4.");
            queries.add("drupal 7.");
        }
        
        return queries;
    }
    
    private void printReport() {
        System.out.println();
        System.out.println(DARK_GREEN + "╔═══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(DARK_GREEN + "║" + WHITE + "                     SCAN RESULTS                           " + DARK_GREEN + "║" + RESET);
        System.out.println(DARK_GREEN + "╚═══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        
        System.out.println(GREEN + "[+] Total devices scanned: " + totalScanned + RESET);
        System.out.println(RED + "[!] Potential vulnerabilities found: " + vulnerabilitiesFound + RESET);
        System.out.println();
        
        // Sort results by severity
        results.sort((a, b) -> Integer.compare(b.severityScore, a.severityScore));
        
        System.out.println(DARK_PINK + "Top Vulnerable Targets:" + RESET);
        System.out.println(DARK_PINK + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        
        int count = 0;
        for (ScanResult result : results) {
            if (count >= 20) break;
            printScanResult(result);
            count++;
        }
        
        System.out.println();
        System.out.println(GREEN + "[*] Scan completed." + RESET);
        System.out.println(GREEN + "[*] Tool created by: " + AUTHOR + RESET);
    }
    
    private void printScanResult(ScanResult result) {
        String severityColor = getSeverityColor(result.severityScore);
        System.out.println(severityColor + "\n[Target] " + RESET + result.query);
        System.out.println(severityColor + "  └─ Devices: " + RESET + result.devicesFound);
        System.out.println(severityColor + "  └─ Vulnerabilities: " + RESET + result.vulnerabilitiesFound);
        System.out.println(severityColor + "  └─ Severity: " + RESET + result.severityLevel + " (" + result.severityScore + "/10)");
        
        if (!result.vulnerabilities.isEmpty()) {
            System.out.println(severityColor + "  └─ Notable CVEs:" + RESET);
            for (String cve : result.vulnerabilities) {
                System.out.println(severityColor + "      • " + RESET + cve);
            }
        }
    }
    
    private String getSeverityColor(int score) {
        if (score >= 8) return RED;
        if (score >= 6) return YELLOW;
        if (score >= 4) return PINK;
        return GREEN;
    }
    
    private class ScanTask implements Callable<ScanResult> {
        private String query;
        
        public ScanTask(String query) {
            this.query = query;
        }
        
        @Override
        public ScanResult call() {
            try {
                // Simulate API call (replace with actual ZoomEye API in production)
                ScanResult result = new ScanResult();
                result.query = query;
                result.devicesFound = simulateSearch(query);
                result.vulnerabilitiesFound = calculateVulnerabilities(query);
                result.severityScore = calculateSeverity(result.vulnerabilitiesFound);
                result.severityLevel = getSeverityLevel(result.severityScore);
                result.vulnerabilities = findKnownVulnerabilities(query);
                
                // Rate limiting
                Thread.sleep(RATE_LIMIT_DELAY);
                
                return result;
            } catch (Exception e) {
                System.err.println(RED + "[-] Scan error for " + query + ": " + e.getMessage() + RESET);
                return null;
            }
        }
        
        private int simulateSearch(String query) {
            // Simulate finding devices (replace with actual API call)
            Random rand = new Random();
            return rand.nextInt(10000) + 100;
        }
        
        private int calculateVulnerabilities(String query) {
            // Calculate based on known vulnerable patterns
            int baseRisk = 0;
            String lowerQuery = query.toLowerCase();
            
            if (lowerQuery.contains("2008") || lowerQuery.contains("xp") || lowerQuery.contains("vista")) {
                baseRisk += 50; // Legacy OS
            }
            if (lowerQuery.contains("apache 2.2") || lowerQuery.contains("nginx 1.4")) {
                baseRisk += 40; // Old web servers
            }
            if (lowerQuery.contains("php 5") || lowerQuery.contains("perl 5")) {
                baseRisk += 45; // Deprecated languages
            }
            if (lowerQuery.contains("cisco ios")) {
                baseRisk += 35; // Network devices
            }
            if (lowerQuery.contains("tomcat 6") || lowerQuery.contains("jboss 5")) {
                baseRisk += 40; // Old app servers
            }
            
            return baseRisk;
        }
        
        private int calculateSeverity(int vulnerabilities) {
            return Math.min(10, (vulnerabilities / 10) + 3);
        }
        
        private String getSeverityLevel(int score) {
            if (score >= 8) return "CRITICAL";
            if (score >= 6) return "HIGH";
            if (score >= 4) return "MEDIUM";
            return "LOW";
        }
        
        private List<String> findKnownVulnerabilities(String query) {
            List<String> cves = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            
            // Known vulnerability database
            if (lowerQuery.contains("nginx 1.4")) {
                cves.add("CVE-2013-2028 (nginx chunked overflow)");
                cves.add("CVE-2014-3556 (nginx range header)");
            }
            if (lowerQuery.contains("apache 2.2")) {
                cves.add("CVE-2014-0098 (apache mod_log_config)");
                cves.add("CVE-2013-2249 (apache mod_session)");
            }
            if (lowerQuery.contains("openssl")) {
                cves.add("CVE-2014-0160 (Heartbleed)");
                cves.add("CVE-2016-0800 (DROWN)");
            }
            if (lowerQuery.contains("php 5")) {
                cves.add("CVE-2014-0167 (PHP glob() overflow)");
                cves.add("CVE-2013-1643 (PHP soap)");
            }
            if (lowerQuery.contains("cisco ios")) {
                cves.add("CVE-2017-3881 (Cisco IOS RCE)");
                cves.add("CVE-2018-0150 (Cisco IOS DoS)");
            }
            if (lowerQuery.contains("windows server 2008")) {
                cves.add("CVE-2017-0144 (EternalBlue)");
                cves.add("CVE-2019-0708 (BlueKeep)");
            }
            if (lowerQuery.contains("openssh 5")) {
                cves.add("CVE-2016-0777 (OpenSSH key leakage)");
            }
            if (lowerQuery.contains("tomcat 6")) {
                cves.add("CVE-2014-0050 (Tomcat DoS)");
                cves.add("CVE-2016-8735 (Tomcat RCE)");
            }
            if (lowerQuery.contains("mysql 5")) {
                cves.add("CVE-2016-6662 (MySQL RCE)");
                cves.add("CVE-2016-6663 (MySQL privilege escalation)");
            }
            if (lowerQuery.contains("iis 6.0")) {
                cves.add("CVE-2017-7269 (IIS RCE)");
            }
            
            return cves;
        }
    }
    
    private class ScanResult {
        String query;
        int devicesFound;
        int vulnerabilitiesFound;
        int severityScore;
        String severityLevel;
        List<String> vulnerabilities = new ArrayList<>();
    }
    
    /**
     * ZoomEye API Integration
     * Replace the simulation methods with actual API calls below
     */
    private static String zoomEyeSearch(String apiKey, String query, int page) throws IOException {
        String url = "https://api.zoomeye.org/host/search?query=" + 
                     URLEncoder.encode(query, StandardCharsets.UTF_8) + 
                     "&page=" + page;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "JWT " + apiKey);
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("API request failed with code: " + responseCode);
        }
    }
    
    private static List<String> parseZoomEyeResults(String jsonResponse) {
        List<String> results = new ArrayList<>();
        
        // Simple regex parsing for device info
        Pattern pattern = Pattern.compile("\"ip\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            results.add(matcher.group(1));
        }
        
        return results;
    }
}
