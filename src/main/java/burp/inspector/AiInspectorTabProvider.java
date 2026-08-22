package burp.inspector;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import burp.storage.StorageManager;

public class AiInspectorTabProvider implements HttpRequestEditorProvider {
    private final MontoyaApi api;
    private final StorageManager storageManager;

    public AiInspectorTabProvider(MontoyaApi api, StorageManager storageManager) {
        this.api = api;
        this.storageManager = storageManager;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        if (!storageManager.getConfig().isEnableInspectorTab()) {
            return null;
        }
        return new AiInspectorTab(api);
    }
}
