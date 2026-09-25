package ai.typesafe.voicebrowser.browser;

import ai.typesafe.voicebrowser.model.ElementSnapshot;
import ai.typesafe.voicebrowser.service.SnapshotCollector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BrowserManager {

    private static final ObjectMapper mapper = new ObjectMapper();

    private Playwright playwright;
    private BrowserContext context;
    private Browser browser;
    private Page activePage;
    private final List<Page> pages = new CopyOnWriteArrayList<>();
    private final List<Consumer<BrowserManager>> listeners = new CopyOnWriteArrayList<>();

    public record TabInfo(int index, String url, boolean active) {}

    public void onChange(Consumer<BrowserManager> listener) {
        listeners.add(listener);
    }

    private void emit() {
        for (Consumer<BrowserManager> listener : listeners) {
            try {
                listener.accept(this);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void launch(boolean headless, String cdp, String profileDirStr, String startUrl) {
        playwright = Playwright.create();
        Path profileDir = Paths.get(profileDirStr != null ? profileDirStr : ".browser-profile");

        if (cdp != null && !cdp.isEmpty()) {
            browser = playwright.chromium().connectOverCDP(cdp);
            List<BrowserContext> contexts = browser.contexts();
            context = !contexts.isEmpty() ? contexts.get(0) : browser.newContext();
        } else {
            BrowserType.LaunchPersistentContextOptions opts = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(headless)
                    .setIgnoreDefaultArgs(List.of("--enable-automation"));

            if (headless) {
                opts.setViewportSize(1280, 900);
            } else {
                opts.setArgs(List.of("--window-size=1280,900", "--window-position=40,40"));
            }
            context = playwright.chromium().launchPersistentContext(profileDir, opts);
        }

        context.addInitScript(OverlayScript.INSTALL_SCRIPT);

        context.onPage(page -> trackPage(page));
        for (Page p : context.pages()) {
            trackPage(p);
        }

        if (pages.isEmpty()) {
            context.newPage();
        }
        activePage = pages.get(pages.size() - 1);

        if (startUrl != null && !startUrl.isEmpty() && !"about:blank".equals(startUrl)) {
            try {
                activePage.navigate(startUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            } catch (Exception ignored) {}
        }
        emit();
    }

    private void trackPage(Page page) {
        if (pages.contains(page)) return;
        pages.add(page);
        activePage = page;

        page.onClose(p -> {
            pages.remove(page);
            if (activePage == page) {
                activePage = !pages.isEmpty() ? pages.get(pages.size() - 1) : null;
            }
            emit();
        });

        page.onFrameNavigated(frame -> {
            if (frame == page.mainFrame()) {
                emit();
            }
        });

        emit();
    }

    public synchronized Page getPage() {
        if (activePage == null || activePage.isClosed()) {
            activePage = pages.stream().filter(p -> !p.isClosed()).findFirst().orElse(null);
        }
        return activePage;
    }

    public synchronized Page ensurePage() {
        Page p = getPage();
        if (p == null) {
            p = context.newPage();
            trackPage(p);
        }
        return p;
    }

    public synchronized void setActive(Page page) {
        activePage = page;
        try {
            page.bringToFront();
        } catch (Exception ignored) {}
        emit();
    }

    public List<Page> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public List<TabInfo> getTabInfo() {
        List<TabInfo> list = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            list.add(new TabInfo(i, p.url(), p == activePage));
        }
        return list;
    }

    public String currentUrl() {
        try {
            return (activePage != null && !activePage.isClosed()) ? activePage.url() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static class SnapshotResult {
        public String url;
        public String title;
        public String site;
        public String searchBoxId;
        public List<ElementSnapshot> elements;
        public List<TabInfo> tabs;
        public String error;
    }

    public SnapshotResult snapshot() {
        Page page = ensurePage();
        SnapshotResult snap = new SnapshotResult();
        snap.tabs = getTabInfo();

        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(1500));
            Object evalResult = page.evaluate(INJECTED_DOM_COLLECTOR);
            String json = mapper.writeValueAsString(evalResult);

            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            snap.url = String.valueOf(map.getOrDefault("url", page.url()));
            snap.title = String.valueOf(map.getOrDefault("title", ""));
            snap.site = SnapshotCollector.detectSite(snap.url);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawElements = (List<Map<String, Object>>) map.get("elements");
            List<ElementSnapshot> elementList = mapper.convertValue(rawElements, new TypeReference<List<ElementSnapshot>>() {});

            snap.elements = SnapshotCollector.compactElements(elementList);
            snap.searchBoxId = SnapshotCollector.findSearchBox(snap.elements);

        } catch (Exception e) {
            snap.url = page.url();
            snap.title = "";
            snap.site = SnapshotCollector.detectSite(snap.url);
            snap.elements = Collections.emptyList();
            snap.error = e.getMessage();
        }

        return snap;
    }

    public void overlay(String fn, Object... args) {
        Page page = getPage();
        if (page == null) return;
        try {
            page.evaluate(OverlayScript.INSTALL_SCRIPT);
            String argsJson = mapper.writeValueAsString(args);
            String evalStr = String.format("if (window.__vb) { window.__vb['%s'].apply(window.__vb, %s); }", fn, argsJson);
            page.evaluate(evalStr);
        } catch (Exception ignored) {}
    }

    public synchronized void close() {
        try {
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {}
    }

    private static final String INJECTED_DOM_COLLECTOR = """
      (() => {
        let vbCounter = 0;
        const VIEWPORT_WIDTH = window.innerWidth;
        const VIEWPORT_HEIGHT = window.innerHeight;
        const INTERACTIVE_ROLES = new Set(["link", "button", "textbox", "combobox", "searchbox", "checkbox", "radio", "tab", "menuitem", "option", "select"]);
        
        const elements = [];
        const walker = document.createTreeWalker(document.body || document.documentElement, NodeFilter.SHOW_ELEMENT);
        let node;
        
        while ((node = walker.nextNode())) {
          if (node.hasAttribute("data-vb-id")) node.removeAttribute("data-vb-id");
          const rect = node.getBoundingClientRect();
          if (rect.width <= 0 || rect.height <= 0) continue;
          
          let role = node.getAttribute("role");
          const tag = node.tagName.toLowerCase();
          if (!role) {
            if (tag === "a" && node.hasAttribute("href")) role = "link";
            else if (tag === "button") role = "button";
            else if (tag === "input") role = node.getAttribute("type") || "textbox";
            else if (tag === "select") role = "select";
            else if (tag === "textarea") role = "textbox";
          }
          if (!role || !INTERACTIVE_ROLES.has(role.toLowerCase())) continue;
          
          vbCounter++;
          const id = "e" + String(vbCounter).padStart(2, "0");
          node.setAttribute("data-vb-id", id);
          
          const inViewport = rect.top >= 0 && rect.top <= VIEWPORT_HEIGHT && rect.left >= 0 && rect.left <= VIEWPORT_WIDTH;
          elements.push({
            id: id,
            tag: tag,
            role: role,
            text: (node.innerText || node.textContent || "").trim().slice(0, 120),
            placeholder: node.getAttribute("placeholder") || "",
            href: node.getAttribute("href") || "",
            type: node.getAttribute("type") || "",
            inputName: node.getAttribute("name") || "",
            inViewport: inViewport,
            top: Math.round(rect.top + window.scrollY),
            left: Math.round(rect.left + window.scrollX)
          });
        }
        return {
          url: window.location.href,
          title: document.title,
          scrollY: Math.round(window.scrollY),
          elements: elements
        };
      })()
    """;
}
