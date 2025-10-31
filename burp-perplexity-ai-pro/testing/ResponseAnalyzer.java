package burp.testing;

import burp.ai.PerplexityClient;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.regex.Pattern;

public class ResponseAnalyzer {
    private final MontoyaApi api;
    private final PerplexityClient perplexityClient;
    
    // Regex patterns for vulnerability detection
    private static final Pattern SQL_ERROR_PATTERN = Pattern.compile(
        "(?i)(SQL syntax|mysql_fetch|ORA-\\d+|PostgreSQL|SQLite|" +
        "Microsoft SQL|ODBC|JDBC|syntax error|pg_query)"
    );
    
    private static final Pattern COMMAND_OUTPUT_PATTERN = Pattern.compile(
        "(?i)(root:|uid=|gid=|bin/bash|/etc/passwd|win.ini|\\[boot loader\\])"
    );
    
    public ResponseAnalyzer(MontoyaApi api, PerplexityClient perplexityClient) {
        this.api = api;
        this.perplexityClient = perplexityClient;
    }
    
    public boolean detectSQLInjection(HttpResponse response) {
        String body = response.bodyToString();
        return SQL_ERROR_PATTERN.matcher(body).find();
    }
    
    public boolean detectCommandInjection(HttpResponse response) {
        String body = response.bodyToString();
        return COMMAND_OUTPUT_PATTERN.matcher(body).find();
    }
    
    public boolean detectXSS(HttpResponse response, String payload) {
        String body = response.bodyToString();
        return body.contains(payload) && !body.contains(htmlEncode(payload));
    }
    
    private String htmlEncode(String input) {
        return input.replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
