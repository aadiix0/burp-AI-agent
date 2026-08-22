package burp.menu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.model.ChatSession;
import burp.ui.MainTabPanel;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final MainTabPanel mainTabPanel;

    public ContextMenuProvider(MontoyaApi api, MainTabPanel mainTabPanel) {
        this.api = api;
        this.mainTabPanel = mainTabPanel;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuList = new ArrayList<>();

        List<HttpRequestResponse> requestResponses = event.selectedRequestResponses();
        if (requestResponses.isEmpty() && event.messageEditorRequestResponse().isPresent()) {
            requestResponses = List.of(event.messageEditorRequestResponse().get().requestResponse());
        }

        if (requestResponses.isEmpty()) {
            return menuList;
        }

        HttpRequestResponse reqRes = requestResponses.get(0);

        JMenu aiMenu = new JMenu("🤖 Send to AI Assistant");

        JMenuItem newSessionItem = new JMenuItem("Send to New AI Session");
        newSessionItem.addActionListener(e -> {
            mainTabPanel.createNewSession();
            attachTrafficToCurrentSession(reqRes);
        });

        JMenuItem activeSessionItem = new JMenuItem("Send to Active AI Session");
        activeSessionItem.addActionListener(e -> {
            attachTrafficToCurrentSession(reqRes);
        });

        aiMenu.add(newSessionItem);
        aiMenu.add(activeSessionItem);

        menuList.add(aiMenu);
        return menuList;
    }

    private void attachTrafficToCurrentSession(HttpRequestResponse reqRes) {
        String url = reqRes.request() != null && reqRes.request().url() != null ? reqRes.request().url() : "";
        String method = reqRes.request() != null && reqRes.request().method() != null ? reqRes.request().method() : "GET";

        String reqStr = reqRes.request() != null && reqRes.request().toByteArray() != null ?
                new String(reqRes.request().toByteArray().getBytes(), StandardCharsets.UTF_8) : "";

        String resStr = reqRes.response() != null && reqRes.response().toByteArray() != null ?
                new String(reqRes.response().toByteArray().getBytes(), StandardCharsets.UTF_8) : "";

        ChatSession activeSession = mainTabPanel.getActiveSession();
        mainTabPanel.attachTrafficToSession(activeSession, url, method, reqStr, resStr);
    }
}
