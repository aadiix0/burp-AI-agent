package burp.ui;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownUtil {
    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        String bodyHtml = RENDERER.render(PARSER.parse(markdown));

        // Modern, clean, high-contrast dark/light compatible CSS styling for Burp
        return "<html><head><style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 13px; line-height: 1.5; padding: 10px; margin: 0; }"
                + "h1, h2, h3, h4 { color: #2060df; font-weight: 600; margin-top: 14px; margin-bottom: 6px; }"
                + "p { margin-top: 0; margin-bottom: 8px; }"
                + "code { font-family: 'Consolas', 'Courier New', monospace; background-color: rgba(120,120,120,0.15); padding: 2px 5px; border-radius: 4px; font-size: 12px; }"
                + "pre { font-family: 'Consolas', 'Courier New', monospace; background-color: #1e1e2e; color: #f8f8f2; padding: 10px; border-radius: 6px; overflow-x: auto; font-size: 12px; border: 1px solid #333; }"
                + "pre code { background-color: transparent; padding: 0; color: inherit; }"
                + "blockquote { border-left: 4px solid #2060df; margin: 0 0 10px 0; padding-left: 10px; opacity: 0.85; }"
                + "ul, ol { margin-top: 0; margin-bottom: 8px; padding-left: 20px; }"
                + "table { border-collapse: collapse; width: 100%; margin-bottom: 10px; }"
                + "th, td { border: 1px solid #444; padding: 6px 10px; text-align: left; font-size: 12px; }"
                + "th { background-color: rgba(120,120,120,0.2); font-weight: bold; }"
                + "</style></head><body>"
                + bodyHtml
                + "</body></html>";
    }
}
