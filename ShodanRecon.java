import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shodan Recon Tool - Finds exposed services for a given target
 * Author: c4l3b
 */
public class ShodanRecon {
    private static final String API_BASE = "https://api.shodan.io";
    private static final String RESET = "\033[0m";
    private static final String SEAFOAM_GREEN = "\033[96m";

    public static void main(String[] args) {
        if (args.length < 1) {
            printColor("Usage: java ShodanRecon <target-ip-or-hostname> [shodan-api-key]");
            printColor("Example: java ShodanRecon 192.168.1.1 YOUR_API_KEY");
            System.exit(1);
        }

        String target = args[0];
        String apiKey = args.length > 1 ? args[1] : System.getenv("SHODAN_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            printColor("[!] Shodan API key required. Pass as argument or set SHODAN_API_KEY env variable");
            System.exit(1);
        }

        printColor("[*] Shodan Recon Tool - Author: c4l3b");
        printColor("[*] Scanning target: " + target);

        try {
            hostSearch(apiKey, target);
        } catch (Exception e) {
            printColor("[!] Error: " + e.getMessage());
        }
    }

    private static void hostSearch(String apiKey, String host) throws Exception {
        String url = API_BASE + "/shodan/host/" + host + "?key=" + apiKey;
        String response = makeRequest(url);

        if (response == null || response.contains("No information") || response.startsWith("Error")) {
            printColor("[!] No information found for: " + host);
            return;
        }

        printColor("\n" + SEAFOAM_GREEN + "=== Results for " + host + " ===" + RESET);

        String ip = extractValue(response, "\"ip_str\"");
        String org = extractValue(response, "\"org\"");
        String os = extractValue(response, "\"os\"");
        String ports = extractValue(response, "\"ports\"");

        if (ip != null) printColor("[*] IP: " + ip);
        if (org != null) printColor("[*] Organization: " + org);
        if (os != null) printColor("[*] OS: " + os);
        if (ports != null) printColor("[*] Open Ports: " + ports);

        // Extract vulns if present
        String vulns = extractJsonArray(response, "\"vulns\"");
        if (vulns != null) {
            printColor("[!] Vulnerabilities: " + vulns);
        }

        // Extract services/banners
        Pattern dataPattern = Pattern.compile("\"data\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher dataMatcher = dataPattern.matcher(response);
        if (dataMatcher.find()) {
            printColor("\n" + SEAFOAM_GREEN + "--- Exposed Services ---" + RESET);
            String[] services = dataMatcher.group(1).split("\\},\\s*\\{");
            int count = 0;
            for (String service : services) {
                if (count >= 20) break;
                String banner = extractValue("{" + service + "}", "\"banner\"");
                String product = extractValue("{" + service + "}", "\"product\"");
                String port = extractValue("{" + service + "}", "\"port\"");
                if (port != null) {
                    printColor("[" + port + "] " + (product != null ? product : "Unknown") + " - " + 
                              (banner != null ? banner.substring(0, Math.min(100, banner.length())) : "N/A"));
                }
                count++;
            }
        }

        printColor("\n[*] Scan complete - Author: c4l3b");
    }

    private static String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(?:\"([^\"]*)\"|(\\d+))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    private static String extractJsonArray(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String makeRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return "Error: " + responseCode;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    private static void printColor(String message) {
        System.out.println(SEAFOAM_GREEN + message + RESET);
    }
}
