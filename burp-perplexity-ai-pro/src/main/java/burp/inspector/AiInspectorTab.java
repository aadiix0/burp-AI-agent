package burp.inspector;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;

import javax.swing.*;
import java.awt.*;

public class AiInspectorTab implements ExtensionProvidedHttpRequestEditor {
    private final MontoyaApi api;
    private final JPanel mainPanel;
    private final JEditorPane displayPane;
    private HttpRequest currentRequest;

    public AiInspectorTab(MontoyaApi api) {
        this.api = api;
        this.mainPanel = new JPanel(new BorderLayout());
        this.displayPane = new JEditorPane();
        this.displayPane.setContentType("text/html");
        this.displayPane.setEditable(false);
        this.displayPane.setText("<html><body><p><i>Right-click and select <b>Send to AI Assistant</b> to analyze this request/response with AI.</i></p></body></html>");
        this.mainPanel.add(new JScrollPane(displayPane), BorderLayout.CENTER);
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        if (requestResponse != null && requestResponse.request() != null) {
            this.currentRequest = requestResponse.request();
            String url = currentRequest.url();
            displayPane.setText("<html><body><h3>AI Analysis Preview</h3><p>Selected Target: <b>" + (url != null ? url : "") + "</b></p><p>Use the context menu to send full request and response data to AI Assistant.</p></body></html>");
        }
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        return true;
    }

    @Override
    public String caption() {
        return "AI Insights";
    }

    @Override
    public Component uiComponent() {
        return mainPanel;
    }

    @Override
    public Selection selectedData() {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public HttpRequest getRequest() {
        return currentRequest;
    }
}
