package ai.typesafe.voicebrowser.browser;

import ai.typesafe.voicebrowser.model.Action;
import ai.typesafe.voicebrowser.service.PolicyEngine;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Executor {

    private static final double NAV_TIMEOUT = 15000;

    public record ExecutionResult(boolean ok, String detail) {}

    private static Locator locatorFor(Page page, String id) {
        return page.locator(String.format("[data-vb-id='%s']", id)).first();
    }

    private static void settle(Page page, long ms) {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(ms));
        } catch (Exception ignored) {}
        try {
            Thread.sleep(120);
        } catch (Exception ignored) {}
    }

    private static void maybeNewTab(BrowserManager browser, List<Page> before) {
        try {
            Thread.sleep(400);
            for (Page p : browser.getPages()) {
                if (!before.contains(p)) {
                    browser.setActive(p);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public static ExecutionResult execute(Action action, BrowserManager browser) {
        Page page = browser.ensurePage();
        String label = PolicyEngine.describe(action);

        switch (action.getType()) {
            case "navigate_url": {
                browser.overlay("toast", "→ " + label);
                String host = "";
                try {
                    host = new URI(action.getUrl()).getHost().replaceFirst("^www\\.", "");
                } catch (Exception ignored) {}

                try {
                    page.navigate(action.getUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(NAV_TIMEOUT));
                } catch (Exception e) {
                    try {
                        Thread.sleep(500);
                        page.navigate(action.getUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT).setTimeout(NAV_TIMEOUT));
                    } catch (Exception ignored) {}
                    if (!page.url().contains(host)) {
                        throw e;
                    }
                }
                settle(page, 800);
                return new ExecutionResult(true, page.url());
            }

            case "click_element": {
                List<Page> before = new ArrayList<>(browser.getPages());
                browser.overlay("clearCandidates");
                browser.overlay("highlight", action.getTargetId(), Constants.HIGHLIGHT_MS);
                browser.overlay("toast", label);
                Locator loc = locatorFor(page, action.getTargetId());

                try {
                    Thread.sleep(180);
                } catch (Exception ignored) {}

                try {
                    loc.click(new Locator.ClickOptions().setTimeout(4000));
                } catch (Exception e) {
                    loc.evaluate("el => el.click()");
                }
                settle(page, 2500);
                maybeNewTab(browser, before);
                return new ExecutionResult(true, browser.getPage().url());
            }

            case "type_into_field": {
                browser.overlay("clearCandidates");
                browser.overlay("highlight", action.getTargetId(), Constants.HIGHLIGHT_MS + 400);
                browser.overlay("toast", label);
                Locator loc = locatorFor(page, action.getTargetId());

                try {
                    loc.click(new Locator.ClickOptions().setTimeout(4000));
                } catch (Exception e) {
                    try { loc.focus(); } catch (Exception ignored) {}
                }

                try { loc.fill(""); } catch (Exception ignored) {}

                try {
                    loc.pressSequentially(action.getText(), new Locator.PressSequentiallyOptions().setDelay(18));
                } catch (Exception e) {
                    loc.fill(action.getText());
                }

                if (Boolean.TRUE.equals(action.getSubmit())) {
                    page.keyboard().press("Enter");
                    settle(page, 2500);
                }
                return new ExecutionResult(true, page.url());
            }

            case "select_option": {
                browser.overlay("highlight", action.getTargetId(), Constants.HIGHLIGHT_MS);
                browser.overlay("toast", label);
                Locator loc = locatorFor(page, action.getTargetId());

                Object picked = loc.evaluate("(sel, wanted) => {" +
                        "  const w = wanted.toLowerCase();" +
                        "  const opts = Array.from(sel.options || []);" +
                        "  const hit = opts.find(o => o.label.toLowerCase() === w) || opts.find(o => o.label.toLowerCase().includes(w));" +
                        "  if (!hit) return null;" +
                        "  sel.value = hit.value;" +
                        "  sel.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "  return hit.label;" +
                        "}", action.getText());

                return new ExecutionResult(picked != null, picked != null ? String.valueOf(picked) : "no matching option");
            }

            case "press_enter": {
                browser.overlay("toast", "⏎ enter");
                page.keyboard().press("Enter");
                settle(page, 2500);
                return new ExecutionResult(true, page.url());
            }

            case "scroll_down":
            case "scroll_up": {
                int dir = "scroll_down".equals(action.getType()) ? 1 : -1;
                browser.overlay("toast", label);
                String amount = action.getAmount() != null ? action.getAmount() : "page";

                page.evaluate(String.format("""
                  (([dir, amount]) => {
                    const vh = window.innerHeight;
                    if (amount === "end") {
                      window.scrollTo({ top: dir > 0 ? document.documentElement.scrollHeight : 0, behavior: "smooth" });
                    } else {
                      const px = amount === "little" ? vh * 0.35 : vh * 0.85;
                      window.scrollBy({ top: dir * px, behavior: "smooth" });
                    }
                  })([%d, "%s"])
                """, dir, amount));

                try { Thread.sleep(350); } catch (Exception ignored) {}
                Object scrollY = page.evaluate("() => Math.round(window.scrollY)");
                return new ExecutionResult(true, "scrollY=" + scrollY);
            }

            case "go_back": {
                browser.overlay("toast", "← back");
                try {
                    page.goBack(new Page.GoBackOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(NAV_TIMEOUT));
                } catch (Exception ignored) {}
                settle(page, 800);
                return new ExecutionResult(true, page.url());
            }

            case "go_forward": {
                browser.overlay("toast", "→ forward");
                try {
                    page.goForward(new Page.GoForwardOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(NAV_TIMEOUT));
                } catch (Exception ignored) {}
                settle(page, 800);
                return new ExecutionResult(true, page.url());
            }

            case "reload": {
                browser.overlay("toast", "↻ reload");
                try {
                    page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(NAV_TIMEOUT));
                } catch (Exception ignored) {}
                return new ExecutionResult(true, page.url());
            }

            case "open_new_tab": {
                Page p = browser.ensurePage();
                browser.setActive(p);
                browser.overlay("toast", "new tab");
                return new ExecutionResult(true, "tabs=" + browser.getPages().size());
            }

            case "close_tab": {
                page.close();
                browser.setActive(browser.getPage());
                return new ExecutionResult(true, "tabs=" + browser.getPages().size());
            }

            case "switch_tab": {
                List<Page> pages = browser.getPages();
                if (pages.size() < 2) return new ExecutionResult(false, "only one tab");
                int idx = pages.indexOf(browser.getPage());
                Page next;
                if ("previous".equals(action.getDirection())) {
                    next = pages.get((idx - 1 + pages.size()) % pages.size());
                } else if ("first".equals(action.getDirection())) {
                    next = pages.get(0);
                } else {
                    next = pages.get((idx + 1) % pages.size());
                }
                browser.setActive(next);
                browser.overlay("toast", "switched tab");
                return new ExecutionResult(true, next.url());
            }

            default:
                return new ExecutionResult(false, "unknown action " + action.getType());
        }
    }

    private static class Constants {
        static final long HIGHLIGHT_MS = 600;
    }
}
