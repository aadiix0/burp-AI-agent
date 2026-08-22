package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.inspector.AiInspectorTabProvider;
import burp.menu.ContextMenuProvider;
import burp.storage.StorageManager;
import burp.ui.MainTabPanel;

public class Extension implements BurpExtension {
    private static MontoyaApi montoyaApi;

    public static MontoyaApi getMontoyaApi() {
        return montoyaApi;
    }

    @Override
    public void initialize(MontoyaApi api) {
        montoyaApi = api;
        api.extension().setName("Burp AI Assistant Pro");

        StorageManager storageManager = new StorageManager(api);
        MainTabPanel mainTabPanel = new MainTabPanel(api, storageManager);

        // Register Suite Main Tab
        api.userInterface().registerSuiteTab("AI Assistant Pro", mainTabPanel);

        // Register Context Menu
        api.userInterface().registerContextMenuItemsProvider(new ContextMenuProvider(api, mainTabPanel));

        // Register Inspector Message Editor Tab
        api.userInterface().registerHttpRequestEditorProvider(new AiInspectorTabProvider(api, storageManager));

        api.logging().logToOutput("Burp AI Assistant Pro loaded successfully!");
    }
}
