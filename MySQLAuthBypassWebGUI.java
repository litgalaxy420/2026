import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.UnsupportedEncodingException;

/**
 * MySQLAuthBypassWebGUI - Blue Team Utility Web Interface
 * Hosts a glossy blue gnome i3 styled web interface on localhost:8080
 * Tests for old MySQL/MariaDB authentication bypass vulnerabilities
 */
public class MySQLAuthBypassWebGUI {

    private static final int PORT = 8080;
    private static MySQLAuthBypassScanner scanner;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  MySQL/MariaDB Auth Bypass Scanner GUI");
        System.out.println("  Blue Team Security Utility");
        System.out.println("===========================================");
        System.out.println("");

        scanner = new MySQLAuthBypassScanner();

        try {
            // Create HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Create thread pool
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            // Add context handlers
            server.createContext("/", new MainHandler());
            server.createContext("/scan", new ScanHandler());
            server.createContext("/results", new ResultsHandler());
            server.createContext("/api/scan", new APIScanHandler());
            server.createContext("/style.css", new CSSHandler());
            server.createContext("/script.js", new JSHandler());
            
            server.start();
            
            System.out.println("[+] MySQL Auth Bypass Scanner GUI Started");
            System.out.println("[+] Access the web interface at: http://localhost:" + PORT);
            System.out.println("[+] Press Ctrl+C to stop the server");
            System.out.println("");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[!] Shutting down server...");
                running = false;
            }));
            
            // Keep main thread alive
            while (running) {
                Thread.sleep(1000);
            }
            
            server.stop(0);
            System.out.println("[+] Server stopped");
            
        } catch (Exception e) {
            System.err.println("[!] Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Main HTML Page Handler
    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getMainHTML();
            sendResponse(exchange, response, "text/html");
        }
    }

    // Scan Form Handler
    static class ScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getScanPageHTML();
            sendResponse(exchange, response, "text/html");
        }
    }

    // Results Display Handler
    static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getResultsPageHTML();
            sendResponse(exchange, response, "text/html");
        }
    }

    // API Handler for scan requests
    static class APIScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                
                // Parse parameters
                String host = extractParam(body, "host");
                String port = extractParam(body, "port");
                String timeout = extractParam(body, "timeout");
                
                int portNum = 3306;
                int timeoutMs = 5000;
                
                try {
                    if (port != null && !port.isEmpty()) {
                        portNum = Integer.parseInt(port);
                    }
                    if (timeout != null && !timeout.isEmpty()) {
                        timeoutMs = Integer.parseInt(timeout) * 1000;
                    }
                } catch (NumberFormatException e) {
                    // Use defaults
                }
                
                MySQLAuthBypassScanner.ScanResult result = scanner.scanTarget(host, portNum, timeoutMs);
                
                // Determine vulnerable status
                result.vulnerable = !result.vulnerabilities.isEmpty();
                
                String jsonResponse = result.toJSON();
                sendResponse(exchange, jsonResponse, "application/json");
            } else {
                sendResponse(exchange, "{\"error\":\"Use POST method\"}", "application/json");
            }
        }
    }

    // CSS Handler
    static class CSSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getGlossyBlueCSS();
            sendResponse(exchange, response, "text/css");
        }
    }

    // JavaScript Handler
    static class JSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getJavaScript();
            sendResponse(exchange, response, "application/javascript");
        }
    }

    // Helper methods
    private static void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static String extractParam(String body, String param) {
        String pattern = param + "=";
        int start = body.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = body.indexOf("&", start);
        if (end == -1) end = body.length();
        try {
            return URLDecoder.decode(body.substring(start, end), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return body.substring(start, end);
        }
    }

    // Get the main HTML page
    private static String getMainHTML() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>MySQL Auth Bypass Scanner - Blue Team Utility</title>\n" +
               "    <link rel=\"stylesheet\" href=\"/style.css\">\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"i3-window\">\n" +
               "        <div class=\"window-bar\">\n" +
               "            <div class=\"window-controls\">\n" +
               "                <span class=\"close-btn\"></span>\n" +
               "                <span class=\"minimize-btn\"></span>\n" +
               "                <span class=\"maximize-btn\"></span>\n" +
               "            </div>\n" +
               "            <div class=\"window-title\">MySQL Auth Bypass Scanner v1.0</div>\n" +
               "        </div>\n" +
               "        <div class=\"window-content\">\n" +
               "            <div class=\"header\">\n" +
               "                <div class=\"logo-icon\">🛡️</div>\n" +
               "                <h1>MySQL/MariaDB Authentication Bypass Scanner</h1>\n" +
               "                <p class=\"subtitle\">Blue Team Security Utility</p>\n" +
               "            </div>\n" +
               "            \n" +
               "            <div class=\"info-box\">\n" +
               "                <h3>🔍 About This Tool</h3>\n" +
               "                <p>This utility tests for old authentication bypass vulnerabilities in MySQL and MariaDB versions, including:</p>\n" +
               "                <ul>\n" +
               "                    <li><strong>CVE-2012-2122</strong> - MariaDB/MySQL Auth Bypass</li>\n" +
               "                    <li><strong>Legacy MySQL 4.x/5.x</strong> - Authentication vulnerabilities</li>\n" +
               "                    <li><strong>Old password hashing</strong> - Security weaknesses</li>\n" +
               "                </ul>\n" +
               "            </div>\n" +
               "            \n" +
               "            <div class=\"scan-form\">\n" +
               "                <h2>🚀 Start New Scan</h2>\n" +
               "                <div class=\"form-group\">\n" +
               "                    <label for=\"host\">Target Host:</label>\n" +
               "                    <input type=\"text\" id=\"host\" name=\"host\" placeholder=\"localhost or IP address\" required>\n" +
               "                </div>\n" +
               "                <div class=\"form-row\">\n" +
               "                    <div class=\"form-group\">\n" +
               "                        <label for=\"port\">Port:</label>\n" +
               "                        <input type=\"number\" id=\"port\" name=\"port\" value=\"3306\" min=\"1\" max=\"65535\">\n" +
               "                    </div>\n" +
               "                    <div class=\"form-group\">\n" +
               "                        <label for=\"timeout\">Timeout (seconds):</label>\n" +
               "                        <input type=\"number\" id=\"timeout\" name=\"timeout\" value=\"5\" min=\"1\" max=\"60\">\n" +
               "                    </div>\n" +
               "                </div>\n" +
               "                <button class=\"scan-btn\" onclick=\"startScan()\">\n" +
               "                    <span class=\"btn-icon\">⚔️</span> Start Vulnerability Scan\n" +
               "                </button>\n" +
               "            </div>\n" +
               "            \n" +
               "            <div id=\"results-container\" class=\"results-container\" style=\"display:none;\">\n" +
               "                <!-- Results will be displayed here -->\n" +
               "            </div>\n" +
               "            \n" +
               "            <div class=\"footer\">\n" +
               "                <p>For Blue Team Security Testing Only | MySQL/MariaDB Vulnerability Scanner</p>\n" +
               "            </div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "    <script src=\"/script.js\"></script>\n" +
               "</body>\n" +
               "</html>";
    }

    private static String getScanPageHTML() {
        return getMainHTML();
    }

    private static String getResultsPageHTML() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Scan Results - MySQL Auth Bypass Scanner</title>\n" +
               "    <link rel=\"stylesheet\" href=\"/style.css\">\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"i3-window\">\n" +
               "        <div class=\"window-bar\">\n" +
               "            <div class=\"window-controls\">\n" +
               "                <span class=\"close-btn\"></span>\n" +
               "                <span class=\"minimize-btn\"></span>\n" +
               "                <span class=\"maximize-btn\"></span>\n" +
               "            </div>\n" +
               "            <div class=\"window-title\">Scan Results</div>\n" +
               "        </div>\n" +
               "        <div class=\"window-content\">\n" +
               "            <div id=\"results-content\"></div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "    <script src=\"/script.js\"></script>\n" +
               "</body>\n" +
               "</html>";
    }

    // Glossy Blue Gnome i3 Style CSS
    private static String getGlossyBlueCSS() {
        return "/* MySQL Auth Bypass Scanner - Glossy Blue Gnome i3 Theme */\n" +
               "\n" +
               "* {\n" +
               "    margin: 0;\n" +
               "    padding: 0;\n" +
               "    box-sizing: border-box;\n" +
               "}\n" +
               "\n" +
               "body {\n" +
               "    font-family: 'Segoe UI', 'Ubuntu', 'Droid Sans', sans-serif;\n" +
               "    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);\n" +
               "    min-height: 100vh;\n" +
               "    display: flex;\n" +
               "    justify-content: center;\n" +
               "    align-items: center;\n" +
               "    padding: 20px;\n" +
               "    color: #e8e8e8;\n" +
               "}\n" +
               "\n" +
               "/* i3 Window Manager Style Window */\n" +
               ".i3-window {\n" +
               "    background: linear-gradient(180deg, #4a90a4 0%, #2c5f72 50%, #1e4a5a 100%);\n" +
               "    border-radius: 12px;\n" +
               "    box-shadow: \n" +
               "        0 0 0 1px rgba(255,255,255,0.1),\n" +
               "        0 4px 20px rgba(0,0,0,0.4),\n" +
               "        inset 0 1px 0 rgba(255,255,255,0.2),\n" +
               "        inset 0 -1px 0 rgba(0,0,0,0.2);\n" +
               "    width: 100%;\n" +
               "    max-width: 900px;\n" +
               "    overflow: hidden;\n" +
               "    position: relative;\n" +
               "}\n" +
               "\n" +
               "/* Glossy effect overlay */\n" +
               ".i3-window::before {\n" +
               "    content: '';\n" +
               "    position: absolute;\n" +
               "    top: 0;\n" +
               "    left: 0;\n" +
               "    right: 0;\n" +
               "    height: 50%;\n" +
               "    background: linear-gradient(180deg, \n" +
               "        rgba(255,255,255,0.15) 0%, \n" +
               "        rgba(255,255,255,0.05) 50%, \n" +
               "        transparent 100%);\n" +
               "    pointer-events: none;\n" +
               "}\n" +
               "\n" +
               "/* Window Title Bar - Gnome/i3 Style */\n" +
               ".window-bar {\n" +
               "    background: linear-gradient(180deg, #3d7a8c 0%, #2d5a6c 100%);\n" +
               "    padding: 8px 12px;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    border-bottom: 1px solid rgba(0,0,0,0.2);\n" +
               "    position: relative;\n" +
               "}\n" +
               "\n" +
               ".window-controls {\n" +
               "    display: flex;\n" +
               "    gap: 8px;\n" +
               "    position: absolute;\n" +
               "    left: 12px;\n" +
               "}\n" +
               "\n" +
               ".window-controls span {\n" +
               "    width: 14px;\n" +
               "    height: 14px;\n" +
               "    border-radius: 50%;\n" +
               "    cursor: pointer;\n" +
               "    box-shadow: inset 0 1px 2px rgba(0,0,0,0.3), 0 1px 0 rgba(255,255,255,0.2);\n" +
               "}\n" +
               "\n" +
               ".close-btn {\n" +
               "    background: linear-gradient(180deg, #ff5f57 0%, #e04440 100%);\n" +
               "}\n" +
               "\n" +
               ".minimize-btn {\n" +
               "    background: linear-gradient(180deg, #ffbd2e 0%, #e09e20 100%);\n" +
               "}\n" +
               "\n" +
               ".maximize-btn {\n" +
               "    background: linear-gradient(180deg, #28c840 0%, #1ea030 100%);\n" +
               "}\n" +
               "\n" +
               ".window-title {\n" +
               "    flex: 1;\n" +
               "    text-align: center;\n" +
               "    font-weight: 600;\n" +
               "    font-size: 14px;\n" +
               "    color: #ffffff;\n" +
               "    text-shadow: 0 1px 2px rgba(0,0,0,0.3);\n" +
               "    letter-spacing: 0.5px;\n" +
               "}\n" +
               "\n" +
               "/* Window Content */\n" +
               ".window-content {\n" +
               "    padding: 24px;\n" +
               "    position: relative;\n" +
               "    z-index: 1;\n" +
               "}\n" +
               "\n" +
               "/* Header Section */\n" +
               ".header {\n" +
               "    text-align: center;\n" +
               "    margin-bottom: 24px;\n" +
               "}\n" +
               "\n" +
               ".logo-icon {\n" +
               "    font-size: 48px;\n" +
               "    margin-bottom: 12px;\n" +
               "    filter: drop-shadow(0 4px 8px rgba(0,0,0,0.3));\n" +
               "}\n" +
               "\n" +
               ".header h1 {\n" +
               "    font-size: 28px;\n" +
               "    font-weight: 700;\n" +
               "    background: linear-gradient(180deg, #ffffff 0%, #b8d4e3 100%);\n" +
               "    -webkit-background-clip: text;\n" +
               "    -webkit-text-fill-color: transparent;\n" +
               "    background-clip: text;\n" +
               "    margin-bottom: 8px;\n" +
               "    text-shadow: none;\n" +
               "}\n" +
               "\n" +
               ".subtitle {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 14px;\n" +
               "    font-weight: 500;\n" +
               "}\n" +
               "\n" +
               "/* Info Box */\n" +
               ".info-box {\n" +
               "    background: linear-gradient(135deg, rgba(74, 144, 164, 0.3) 0%, rgba(44, 95, 114, 0.3) 100%);\n" +
               "    border: 1px solid rgba(122, 184, 212, 0.3);\n" +
               "    border-radius: 8px;\n" +
               "    padding: 16px 20px;\n" +
               "    margin-bottom: 24px;\n" +
               "}\n" +
               "\n" +
               ".info-box h3 {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 16px;\n" +
               "    margin-bottom: 10px;\n" +
               "}\n" +
               "\n" +
               ".info-box p {\n" +
               "    color: #a8c8d8;\n" +
               "    font-size: 13px;\n" +
               "    line-height: 1.6;\n" +
               "    margin-bottom: 10px;\n" +
               "}\n" +
               "\n" +
               ".info-box ul {\n" +
               "    list-style: none;\n" +
               "    padding-left: 0;\n" +
               "}\n" +
               "\n" +
               ".info-box li {\n" +
               "    color: #a8c8d8;\n" +
               "    font-size: 13px;\n" +
               "    padding: 4px 0;\n" +
               "    padding-left: 20px;\n" +
               "    position: relative;\n" +
               "}\n" +
               "\n" +
               ".info-box li::before {\n" +
               "    content: '▸';\n" +
               "    position: absolute;\n" +
               "    left: 0;\n" +
               "    color: #4a90a4;\n" +
               "}\n" +
               "\n" +
               "/* Scan Form */\n" +
               ".scan-form {\n" +
               "    background: linear-gradient(135deg, rgba(74, 144, 164, 0.2) 0%, rgba(44, 95, 114, 0.2) 100%);\n" +
               "    border: 1px solid rgba(122, 184, 212, 0.2);\n" +
               "    border-radius: 10px;\n" +
               "    padding: 24px;\n" +
               "    margin-bottom: 24px;\n" +
               "}\n" +
               "\n" +
               ".scan-form h2 {\n" +
               "    color: #ffffff;\n" +
               "    font-size: 20px;\n" +
               "    margin-bottom: 20px;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    gap: 10px;\n" +
               "}\n" +
               "\n" +
               ".form-group {\n" +
               "    margin-bottom: 16px;\n" +
               "}\n" +
               "\n" +
               ".form-row {\n" +
               "    display: flex;\n" +
               "    gap: 16px;\n" +
               "}\n" +
               "\n" +
               ".form-row .form-group {\n" +
               "    flex: 1;\n" +
               "}\n" +
               "\n" +
               "label {\n" +
               "    display: block;\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 13px;\n" +
               "    font-weight: 600;\n" +
               "    margin-bottom: 6px;\n" +
               "    text-transform: uppercase;\n" +
               "    letter-spacing: 0.5px;\n" +
               "}\n" +
               "\n" +
               "input[type=\"text\"],\n" +
               "input[type=\"number\"] {\n" +
               "    width: 100%;\n" +
               "    padding: 12px 16px;\n" +
               "    background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.3) 100%);\n" +
               "    border: 1px solid rgba(122, 184, 212, 0.3);\n" +
               "    border-radius: 6px;\n" +
               "    color: #ffffff;\n" +
               "    font-size: 14px;\n" +
               "    outline: none;\n" +
               "    transition: all 0.3s ease;\n" +
               "}\n" +
               "\n" +
               "input[type=\"text\"]:focus,\n" +
               "input[type=\"number\"]:focus {\n" +
               "    border-color: #7ab8d4;\n" +
               "    box-shadow: 0 0 10px rgba(122, 184, 212, 0.3);\n" +
               "    background: linear-gradient(180deg, rgba(0,0,0,0.25) 0%, rgba(0,0,0,0.35) 100%);\n" +
               "}\n" +
               "\n" +
               "input[type=\"text\"]::placeholder {\n" +
               "    color: #5a7a8a;\n" +
               "}\n" +
               "\n" +
               "/* Scan Button - Glossy Blue */\n" +
               ".scan-btn {\n" +
               "    width: 100%;\n" +
               "    padding: 14px 24px;\n" +
               "    background: linear-gradient(180deg, #4a90a4 0%, #2c5f72 50%, #1e4a5a 100%);\n" +
               "    border: 1px solid rgba(255,255,255,0.1);\n" +
               "    border-radius: 8px;\n" +
               "    color: #ffffff;\n" +
               "    font-size: 16px;\n" +
               "    font-weight: 600;\n" +
               "    cursor: pointer;\n" +
               "    display: flex;\n" +
               "    justify-content: center;\n" +
               "    align-items: center;\n" +
               "    gap: 10px;\n" +
               "    transition: all 0.3s ease;\n" +
               "    box-shadow: \n" +
               "        0 2px 10px rgba(0,0,0,0.3),\n" +
               "        inset 0 1px 0 rgba(255,255,255,0.2);\n" +
               "}\n" +
               "\n" +
               ".scan-btn:hover {\n" +
               "    background: linear-gradient(180deg, #5aa0b4 0%, #3c6f82 50%, #2c5a6a 100%);\n" +
               "    transform: translateY(-2px);\n" +
               "    box-shadow: \n" +
               "        0 4px 15px rgba(0,0,0,0.4),\n" +
               "        inset 0 1px 0 rgba(255,255,255,0.3);\n" +
               "}\n" +
               "\n" +
               ".scan-btn:active {\n" +
               "    transform: translateY(0);\n" +
               "    background: linear-gradient(180deg, #2c5f72 0%, #1e4a5a 100%);\n" +
               "}\n" +
               "\n" +
               ".scan-btn:disabled {\n" +
               "    opacity: 0.6;\n" +
               "    cursor: not-allowed;\n" +
               "    transform: none;\n" +
               "}\n" +
               "\n" +
               ".btn-icon {\n" +
               "    font-size: 18px;\n" +
               "}\n" +
               "\n" +
               "/* Results Container */\n" +
               ".results-container {\n" +
               "    margin-top: 24px;\n" +
               "}\n" +
               "\n" +
               ".result-card {\n" +
               "    background: linear-gradient(135deg, rgba(74, 144, 164, 0.25) 0%, rgba(44, 95, 114, 0.25) 100%);\n" +
               "    border: 1px solid rgba(122, 184, 212, 0.25);\n" +
               "    border-radius: 10px;\n" +
               "    padding: 20px;\n" +
               "    margin-bottom: 16px;\n" +
               "}\n" +
               "\n" +
               ".result-header {\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    gap: 12px;\n" +
               "    margin-bottom: 16px;\n" +
               "}\n" +
               "\n" +
               ".status-icon {\n" +
               "    font-size: 32px;\n" +
               "}\n" +
               "\n" +
               ".result-title {\n" +
               "    flex: 1;\n" +
               "}\n" +
               "\n" +
               ".result-title h3 {\n" +
               "    color: #ffffff;\n" +
               "    font-size: 18px;\n" +
               "    margin-bottom: 4px;\n" +
               "}\n" +
               "\n" +
               ".result-title span {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 13px;\n" +
               "}\n" +
               "\n" +
               ".status-badge {\n" +
               "    padding: 6px 16px;\n" +
               "    border-radius: 20px;\n" +
               "    font-size: 12px;\n" +
               "    font-weight: 600;\n" +
               "    text-transform: uppercase;\n" +
               "    letter-spacing: 1px;\n" +
               "}\n" +
               "\n" +
               ".status-safe {\n" +
               "    background: linear-gradient(180deg, #28c840 0%, #1ea030 100%);\n" +
               "    color: white;\n" +
               "    box-shadow: 0 2px 10px rgba(40, 200, 64, 0.3);\n" +
               "}\n" +
               "\n" +
               ".status-danger {\n" +
               "    background: linear-gradient(180deg, #ff5f57 0%, #e04440 100%);\n" +
               "    color: white;\n" +
               "    box-shadow: 0 2px 10px rgba(255, 95, 87, 0.3);\n" +
               "}\n" +
               "\n" +
               ".status-warning {\n" +
               "    background: linear-gradient(180deg, #ffbd2e 0%, #e09e20 100%);\n" +
               "    color: white;\n" +
               "    box-shadow: 0 2px 10px rgba(255, 189, 46, 0.3);\n" +
               "}\n" +
               "\n" +
               ".result-details {\n" +
               "    display: grid;\n" +
               "    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
               "    gap: 12px;\n" +
               "    margin-bottom: 16px;\n" +
               "}\n" +
               "\n" +
               ".detail-item {\n" +
               "    background: rgba(0,0,0,0.15);\n" +
               "    padding: 12px;\n" +
               "    border-radius: 6px;\n" +
               "}\n" +
               "\n" +
               ".detail-label {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 11px;\n" +
               "    text-transform: uppercase;\n" +
               "    letter-spacing: 0.5px;\n" +
               "    margin-bottom: 4px;\n" +
               "}\n" +
               "\n" +
               ".detail-value {\n" +
               "    color: #ffffff;\n" +
               "    font-size: 14px;\n" +
               "    font-weight: 500;\n" +
               "    word-break: break-all;\n" +
               "}\n" +
               "\n" +
               ".vulnerabilities-section {\n" +
               "    margin-top: 16px;\n" +
               "}\n" +
               "\n" +
               ".vuln-item {\n" +
               "    background: rgba(255, 95, 87, 0.15);\n" +
               "    border: 1px solid rgba(255, 95, 87, 0.3);\n" +
               "    border-radius: 8px;\n" +
               "    padding: 16px;\n" +
               "    margin-bottom: 12px;\n" +
               "}\n" +
               "\n" +
               ".vuln-header {\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    gap: 10px;\n" +
               "    margin-bottom: 8px;\n" +
               "}\n" +
               "\n" +
               ".vuln-icon {\n" +
               "    font-size: 20px;\n" +
               "}\n" +
               "\n" +
               ".vuln-title {\n" +
               "    color: #ff8a85;\n" +
               "    font-weight: 600;\n" +
               "    font-size: 16px;\n" +
               "}\n" +
               "\n" +
               ".vuln-severity {\n" +
               "    margin-left: auto;\n" +
               "    padding: 4px 10px;\n" +
               "    border-radius: 4px;\n" +
               "    font-size: 11px;\n" +
               "    font-weight: 600;\n" +
               "    text-transform: uppercase;\n" +
               "}\n" +
               "\n" +
               ".severity-high {\n" +
               "    background: #e04440;\n" +
               "    color: white;\n" +
               "}\n" +
               "\n" +
               ".severity-medium {\n" +
               "    background: #e09e20;\n" +
               "    color: white;\n" +
               "}\n" +
               "\n" +
               ".severity-low {\n" +
               "    background: #28c840;\n" +
               "    color: white;\n" +
               "}\n" +
               "\n" +
               ".vuln-description {\n" +
               "    color: #ffb3b0;\n" +
               "    font-size: 13px;\n" +
               "    line-height: 1.6;\n" +
               "    margin-bottom: 8px;\n" +
               "}\n" +
               "\n" +
               ".recommendations-section {\n" +
               "    margin-top: 16px;\n" +
               "    padding-top: 16px;\n" +
               "    border-top: 1px solid rgba(122, 184, 212, 0.2);\n" +
               "}\n" +
               "\n" +
               ".recommendations-section h4 {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 14px;\n" +
               "    margin-bottom: 12px;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    gap: 8px;\n" +
               "}\n" +
               "\n" +
               ".recommendation-item {\n" +
               "    background: rgba(40, 200, 64, 0.1);\n" +
               "    border-left: 3px solid #28c840;\n" +
               "    padding: 10px 14px;\n" +
               "    margin-bottom: 8px;\n" +
               "    border-radius: 0 6px 6px 0;\n" +
               "    color: #a8d8a8;\n" +
               "    font-size: 13px;\n" +
               "}\n" +
               "\n" +
               "/* Scan Log */\n" +
               ".scan-log {\n" +
               "    background: rgba(0,0,0,0.3);\n" +
               "    border-radius: 8px;\n" +
               "    padding: 16px;\n" +
               "    margin-top: 16px;\n" +
               "    max-height: 300px;\n" +
               "    overflow-y: auto;\n" +
               "}\n" +
               "\n" +
               ".scan-log h4 {\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 14px;\n" +
               "    margin-bottom: 12px;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    gap: 8px;\n" +
               "}\n" +
               "\n" +
               ".log-entry {\n" +
               "    font-family: 'Courier New', monospace;\n" +
               "    font-size: 12px;\n" +
               "    color: #a8c8d8;\n" +
               "    padding: 4px 0;\n" +
               "    border-bottom: 1px solid rgba(122, 184, 212, 0.1);\n" +
               "}\n" +
               "\n" +
               ".log-entry:last-child {\n" +
               "    border-bottom: none;\n" +
               "}\n" +
               "\n" +
               "/* Loading Animation */\n" +
               ".loading {\n" +
               "    display: flex;\n" +
               "    flex-direction: column;\n" +
               "    align-items: center;\n" +
               "    padding: 40px;\n" +
               "}\n" +
               "\n" +
               ".spinner {\n" +
               "    width: 50px;\n" +
               "    height: 50px;\n" +
               "    border: 4px solid rgba(122, 184, 212, 0.2);\n" +
               "    border-top-color: #4a90a4;\n" +
               "    border-radius: 50%;\n" +
               "    animation: spin 1s linear infinite;\n" +
               "}\n" +
               "\n" +
               "@keyframes spin {\n" +
               "    to { transform: rotate(360deg); }\n" +
               "}\n" +
               "\n" +
               ".loading-text {\n" +
               "    margin-top: 16px;\n" +
               "    color: #7ab8d4;\n" +
               "    font-size: 14px;\n" +
               "}\n" +
               "\n" +
               "/* Footer */\n" +
               ".footer {\n" +
               "    text-align: center;\n" +
               "    padding-top: 20px;\n" +
               "    border-top: 1px solid rgba(122, 184, 212, 0.15);\n" +
               "    margin-top: 20px;\n" +
               "}\n" +
               "\n" +
               ".footer p {\n" +
               "    color: #5a7a8a;\n" +
               "    font-size: 12px;\n" +
               "}\n" +
               "\n" +
               "/* Scrollbar Styling */\n" +
               "::-webkit-scrollbar {\n" +
               "    width: 8px;\n" +
               "    height: 8px;\n" +
               "}\n" +
               "\n" +
               "::-webkit-scrollbar-track {\n" +
               "    background: rgba(0,0,0,0.2);\n" +
               "    border-radius: 4px;\n" +
               "}\n" +
               "\n" +
               "::-webkit-scrollbar-thumb {\n" +
               "    background: rgba(122, 184, 212, 0.4);\n" +
               "    border-radius: 4px;\n" +
               "}\n" +
               "\n" +
               "::-webkit-scrollbar-thumb:hover {\n" +
               "    background: rgba(122, 184, 212, 0.6);\n" +
               "}\n" +
               "\n" +
               "/* Responsive Design */\n" +
               "@media (max-width: 768px) {\n" +
               "    body {\n" +
               "        padding: 10px;\n" +
               "    }\n" +
               "    \n" +
               "    .window-content {\n" +
               "        padding: 16px;\n" +
               "    }\n" +
               "    \n" +
               "    .header h1 {\n" +
               "        font-size: 22px;\n" +
               "    }\n" +
               "    \n" +
               "    .form-row {\n" +
               "        flex-direction: column;\n" +
               "        gap: 0;\n" +
               "    }\n" +
               "    \n" +
               "    .result-details {\n" +
               "        grid-template-columns: 1fr;\n" +
               "    }\n" +
               "}";
    }

    // JavaScript for the web interface
    private static String getJavaScript() {
        return "/* MySQL Auth Bypass Scanner - Web Interface JavaScript */\n" +
               "\n" +
               "async function startScan() {\n" +
               "    const hostInput = document.getElementById('host');\n" +
               "    const portInput = document.getElementById('port');\n" +
               "    const timeoutInput = document.getElementById('timeout');\n" +
               "    const resultsContainer = document.getElementById('results-container');\n" +
               "    const scanBtn = document.querySelector('.scan-btn');\n" +
               "    \n" +
               "    const host = hostInput.value.trim();\n" +
               "    const port = portInput.value.trim() || '3306';\n" +
               "    const timeout = timeoutInput.value.trim() || '5';\n" +
               "    \n" +
               "    if (!host) {\n" +
               "        showNotification('Please enter a target host', 'error');\n" +
               "        return;\n" +
               "    }\n" +
               "    \n" +
               "    // Show loading state\n" +
               "    scanBtn.disabled = true;\n" +
               "    scanBtn.innerHTML = '<span class=\"spinner-small\"></span> Scanning...';\n" +
               "    \n" +
               "    resultsContainer.style.display = 'block';\n" +
               "    resultsContainer.innerHTML = getLoadingHTML();\n" +
               "    \n" +
               "    try {\n" +
               "        const response = await fetch('/api/scan', {\n" +
               "            method: 'POST',\n" +
               "            headers: {\n" +
               "                'Content-Type': 'application/x-www-form-urlencoded'\n" +
               "            },\n" +
               "            body: `host=${encodeURIComponent(host)}&port=${encodeURIComponent(port)}&timeout=${encodeURIComponent(timeout)}`\n" +
               "        });\n" +
               "        \n" +
               "        if (!response.ok) {\n" +
               "            throw new Error('Scan failed: ' + response.statusText);\n" +
               "        }\n" +
               "        \n" +
               "        const result = await response.json();\n" +
               "        displayResults(result);\n" +
               "        \n" +
               "    } catch (error) {\n" +
               "        resultsContainer.innerHTML = getErrorHTML(error.message);\n" +
               "    } finally {\n" +
               "        scanBtn.disabled = false;\n" +
               "        scanBtn.innerHTML = '<span class=\"btn-icon\">⚔️</span> Start Vulnerability Scan';\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "function displayResults(result) {\n" +
               "    const resultsContainer = document.getElementById('results-container');\n" +
               "    \n" +
               "    const isVulnerable = result.vulnerable;\n" +
               "    const statusClass = isVulnerable ? 'status-danger' : 'status-safe';\n" +
               "    const statusText = isVulnerable ? 'VULNERABLE' : 'SECURE';\n" +
               "    const statusIcon = isVulnerable ? '☠️' : '✅';\n" +
               "    const titleColor = isVulnerable ? '#ff8a85' : '#a8d8a8';\n" +
               "    \n" +
               "    let html = `\n" +
               "        <div class=\"result-card\">\n" +
               "            <div class=\"result-header\">\n" +
               "                <div class=\"status-icon\">${statusIcon}</div>\n" +
               "                <div class=\"result-title\">\n" +
               "                    <h3 style=\"color: ${titleColor}\">Scan Results</h3>\n" +
               "                    <span>Target: ${escapeHtml(result.target)}</span>\n" +
               "                </div>\n" +
               "                <div class=\"status-badge ${statusClass}\">${statusText}</div>\n" +
               "            </div>\n" +
               "            \n" +
               "            <div class=\"result-details\">\n" +
               "                <div class=\"detail-item\">\n" +
               "                    <div class=\"detail-label\">Detected Version</div>\n" +
               "                    <div class=\"detail-value\">${escapeHtml(result.version || 'Unknown')}</div>\n" +
               "                </div>\n" +
               "                <div class=\"detail-item\">\n" +
               "                    <div class=\"detail-label\">Target</div>\n" +
               "                    <div class=\"detail-value\">${escapeHtml(result.target)}</div>\n" +
               "                </div>\n" +
               "                <div class=\"detail-item\">\n" +
               "                    <div class=\"detail-label\">Vulnerabilities Found</div>\n" +
               "                    <div class=\"detail-value\">${result.vulnerabilities ? result.vulnerabilities.length : 0}</div>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "    `;\n" +
               "    \n" +
               "    // Display vulnerabilities\n" +
               "    if (result.vulnerabilities && result.vulnerabilities.length > 0) {\n" +
               "        html += '<div class=\"vulnerabilities-section\">';\n" +
               "        html += '<h3 style=\"color: #ff8a85; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;\">⚠️ Detected Vulnerabilities</h3>';\n" +
               "        \n" +
               "        for (const vuln of result.vulnerabilities) {\n" +
               "            html += `\n" +
               "                <div class=\"vuln-item\">\n" +
               "                    <div class=\"vuln-header\">\n" +
               "                        <span class=\"vuln-icon\">☠️</span>\n" +
               "                        <span class=\"vuln-title\">${escapeHtml(vuln)}</span>\n" +
               "                        <span class=\"vuln-severity severity-high\">HIGH</span>\n" +
               "                    </div>\n" +
               "                    <div class=\"vuln-description\">\n" +
               "                        This vulnerability allows potential attackers to bypass authentication mechanisms.\n" +
               "                        Immediate patching and security review is recommended.\n" +
               "                    </div>\n" +
               "                </div>\n" +
               "            `;\n" +
               "        }\n" +
               "        html += '</div>';\n" +
               "    }\n" +
               "    \n" +
               "    // Display recommendations\n" +
               "    html += '<div class=\"recommendations-section\">';\n" +
               "    html += '<h4>💡 Security Recommendations</h4>';\n" +
               "    \n" +
               "    if (isVulnerable) {\n" +
               "        html += '<div class=\"recommendation-item\">🚨 URGENT: Upgrade MySQL/MariaDB to the latest version</div>';\n" +
               "        html += '<div class=\"recommendation-item\">🔐 Review and update all user passwords immediately</div>';\n" +
               "        html += '<div class=\"recommendation-item\">🛡️ Enable SSL/TLS for database connections</div>';\n" +
               "        html += '<div class=\"recommendation-item\">📋 Audit user privileges and access controls</div>';\n" +
               "    } else {\n" +
               "        html += '<div class=\"recommendation-item\">✅ No critical vulnerabilities detected</div>';\n" +
               "        html += '<div class=\"recommendation-item\">🔄 Keep MySQL/MariaDB updated with security patches</div>';\n" +
               "        html += '<div class=\"recommendation-item\">🔐 Use strong authentication mechanisms</div>';\n" +
               "        html += '<div class=\"recommendation-item\">📋 Continue regular security audits</div>';\n" +
               "    }\n" +
               "    \n" +
               "    html += '</div>';\n" +
               "    \n" +
               "    // Display scan log\n" +
               "    if (result.scanLog && result.scanLog.length > 0) {\n" +
               "        html += `\n" +
               "            <div class=\"scan-log\">\n" +
               "                <h4>📝 Scan Log</h4>\n" +
               "        `;\n" +
               "        \n" +
               "        for (const log of result.scanLog) {\n" +
               "            html += `<div class=\"log-entry\">${escapeHtml(log)}</div>`;\n" +
               "        }\n" +
               "        \n" +
               "        html += '</div>';\n" +
               "    }\n" +
               "    \n" +
               "    html += '</div>';\n" +
               "    \n" +
               "    resultsContainer.innerHTML = html;\n" +
               "    \n" +
               "    // Scroll to results\n" +
               "    resultsContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });\n" +
               "}\n" +
               "\n" +
               "function getLoadingHTML() {\n" +
               "    return `\n" +
               "        <div class=\"loading\">\n" +
               "            <div class=\"spinner\"></div>\n" +
               "            <div class=\"loading-text\">Scanning target for vulnerabilities...</div>\n" +
               "        </div>\n" +
               "    `;\n" +
               "}\n" +
               "\n" +
               "function getErrorHTML(message) {\n" +
               "    return `\n" +
               "        <div class=\"result-card\">\n" +
               "            <div class=\"result-header\">\n" +
               "                <div class=\"status-icon\">❌</div>\n" +
               "                <div class=\"result-title\">\n" +
               "                    <h3 style=\"color: #ff8a85;\">Scan Error</h3>\n" +
               "                    <span>An error occurred during the scan</span>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "            <div style=\"color: #ffb3b0; padding: 16px;\">\n" +
               "                ${escapeHtml(message)}\n" +
               "            </div>\n" +
               "        </div>\n" +
               "    `;\n" +
               "}\n" +
               "\n" +
               "function showNotification(message, type) {\n" +
               "    const notification = document.createElement('div');\n" +
               "    notification.className = `notification ${type}`;\n" +
               "    notification.style.cssText = `\n" +
               "        position: fixed;\n" +
               "        top: 20px;\n" +
               "        right: 20px;\n" +
               "        padding: 16px 24px;\n" +
               "        border-radius: 8px;\n" +
               "        color: white;\n" +
               "        font-weight: 500;\n" +
               "        z-index: 1000;\n" +
               "        animation: slideIn 0.3s ease;\n" +
               "        ${type === 'error' ? 'background: linear-gradient(180deg, #e04440, #c03030);' : 'background: linear-gradient(180deg, #28c840, #1ea030);'}\n" +
               "    `;\n" +
               "    notification.textContent = message;\n" +
               "    \n" +
               "    document.body.appendChild(notification);\n" +
               "    \n" +
               "    setTimeout(() => {\n" +
               "        notification.style.animation = 'slideOut 0.3s ease';\n" +
               "        setTimeout(() => notification.remove(), 300);\n" +
               "    }, 3000);\n" +
               "}\n" +
               "\n" +
               "function escapeHtml(text) {\n" +
               "    if (!text) return '';\n" +
               "    const div = document.createElement('div');\n" +
               "    div.textContent = text;\n" +
               "    return div.innerHTML;\n" +
               "}\n" +
               "\n" +
               "// Add keyboard shortcut\n" +
               "document.addEventListener('keypress', function(e) {\n" +
               "    if (e.key === 'Enter' && e.ctrlKey) {\n" +
               "        startScan();\n" +
               "    }\n" +
               "});\n" +
               "\n" +
               "// Add animation keyframes dynamically\n" +
               "const styleSheet = document.createElement('style');\n" +
               "styleSheet.textContent = `\n" +
               "    @keyframes slideIn {\n" +
               "        from { transform: translateX(100%); opacity: 0; }\n" +
               "        to { transform: translateX(0); opacity: 1; }\n" +
               "    }\n" +
               "    @keyframes slideOut {\n" +
               "        from { transform: translateX(0); opacity: 1; }\n" +
               "        to { transform: translateX(100%); opacity: 0; }\n" +
               "    }\n" +
               "    .spinner-small {\n" +
               "        display: inline-block;\n" +
               "        width: 16px;\n" +
               "        height: 16px;\n" +
               "        border: 2px solid rgba(255,255,255,0.3);\n" +
               "        border-top-color: white;\n" +
               "        border-radius: 50%;\n" +
               "        animation: spin 0.8s linear infinite;\n" +
               "    }\n" +
               "`;\n" +
               "document.head.appendChild(styleSheet);\n" +
               "\n" +
               "console.log('MySQL Auth Bypass Scanner loaded successfully');";
    }
}
