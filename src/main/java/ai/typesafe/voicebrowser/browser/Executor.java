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
                Page p = browser.newTab();
                browser.setActive(p);
                browser.overlay("toast", "new tab");
                return new ExecutionResult(true, "tabs=" + browser.getPages().size());
            }

            case "select_ordinal": {
                int targetIdx = action.getOrdinalIndex() != null ? action.getOrdinalIndex() - 1 : 0;
                String cat = action.getTargetCategory() != null ? action.getTargetCategory() : "video";
                browser.overlay("toast", "🎯 select " + (targetIdx + 1) + " " + cat);
                List<Page> before = new ArrayList<>(browser.getPages());

                Object success = page.evaluate("(([targetIdx, cat]) => {" +
                        "  let sel = 'ytd-video-renderer a#video-title, ytd-rich-item-renderer a#video-title, ytd-compact-video-renderer a#video-title, ytd-compact-video-renderer a.yt-simple-endpoint, a[href*=\"/watch?v=\"]';" +
                        "  if (cat === 'link') sel = 'a[href]';" +
                        "  else if (cat === 'button') sel = 'button, [role=\"button\"]';" +
                        "  const rawItems = Array.from(document.querySelectorAll(sel));" +
                        "  const seenHrefs = new Set();" +
                        "  const items = [];" +
                        "  for (const el of rawItems) {" +
                        "    const r = el.getBoundingClientRect();" +
                        "    if (!(r.width > 0 && r.height > 0 && window.getComputedStyle(el).visibility !== 'hidden')) continue;" +
                        "    const href = el.getAttribute('href') || '';" +
                        "    if (cat === 'video' && href.includes('/watch?v=')) {" +
                        "      const vId = href.split('/watch?v=')[1]?.split('&')[0];" +
                        "      if (vId) {" +
                        "        if (seenHrefs.has(vId)) continue;" +
                        "        seenHrefs.add(vId);" +
                        "      }" +
                        "    }" +
                        "    items.push(el);" +
                        "  }" +
                        "  if (items.length <= targetIdx) return null;" +
                        "  const target = items[targetIdx];" +
                        "  target.scrollIntoView({ behavior: 'smooth', block: 'center' });" +
                        "  target.click();" +
                        "  return target.getAttribute('href') || target.getAttribute('title') || target.innerText || 'clicked';" +
                        "})", List.of(targetIdx, cat));

                settle(page, 2500);
                maybeNewTab(browser, before);
                return new ExecutionResult(success != null, success != null ? String.valueOf(success) : "ordinal item not found");
            }

            case "playback_speed": {
                double rate = action.getPlaybackRate() != null ? action.getPlaybackRate() : 1.0;
                browser.overlay("toast", "⚡ speed " + rate + "x");
                Object speedRes = page.evaluate("([rate]) => {" +
                        "  const v = document.querySelector('video, audio');" +
                        "  if (!v) return 'no media element found';" +
                        "  v.playbackRate = rate;" +
                        "  return 'playbackRate=' + v.playbackRate + 'x';" +
                        "}", List.of(rate));
                return new ExecutionResult(true, String.valueOf(speedRes));
            }

            case "youtube_action": {
                String cmd = action.getYtCommand() != null ? action.getYtCommand() : "cc";
                browser.overlay("toast", "📺 youtube " + cmd);

                switch (cmd) {
                    case "cc":
                        page.keyboard().press("c");
                        return new ExecutionResult(true, "toggled captions (c)");
                    case "theater":
                        page.keyboard().press("t");
                        return new ExecutionResult(true, "toggled theater mode (t)");
                    case "miniplayer":
                        page.keyboard().press("i");
                        return new ExecutionResult(true, "toggled miniplayer (i)");
                    case "next_chapter":
                        page.keyboard().press("Control+ArrowRight");
                        return new ExecutionResult(true, "next chapter (Ctrl+Right)");
                    case "prev_chapter":
                        page.keyboard().press("Control+ArrowLeft");
                        return new ExecutionResult(true, "previous chapter (Ctrl+Left)");
                    case "comments":
                        page.evaluate("() => {" +
                                "  const comments = document.querySelector('ytd-comments, #comments');" +
                                "  if (comments) comments.scrollIntoView({ behavior: 'smooth' });" +
                                "  else window.scrollBy({ top: 600, behavior: 'smooth' });" +
                                "}");
                        return new ExecutionResult(true, "scrolled to comments");
                    case "like":
                        Object likeRes = page.evaluate("() => {" +
                                "  const btn = document.querySelector('like-button-view-model button, ytd-toggle-button-renderer button');" +
                                "  if (!btn) return 'like button not found';" +
                                "  btn.click();" +
                                "  return 'liked video';" +
                                "}");
                        return new ExecutionResult(true, String.valueOf(likeRes));
                    case "subscribe":
                        Object subRes = page.evaluate("() => {" +
                                "  const btn = document.querySelector('ytd-subscribe-button-renderer button, #subscribe-button button');" +
                                "  if (!btn) return 'subscribe button not found';" +
                                "  btn.click();" +
                                "  return 'subscribed';" +
                                "}");
                        return new ExecutionResult(true, String.valueOf(subRes));
                    default:
                        return new ExecutionResult(false, "unknown youtube action " + cmd);
                }
            }

            case "media_control": {
                String cmd = action.getMediaCommand() != null ? action.getMediaCommand() : "toggle";
                browser.overlay("toast", "⏯ media " + cmd);

                if ("volume_up".equals(cmd) || "volume_down".equals(cmd)) {
                    double delta = action.getVolumeLevel() != null ? action.getVolumeLevel() : ("volume_up".equals(cmd) ? 0.15 : -0.15);
                    Object volRes = page.evaluate("([delta]) => {" +
                            "  const v = document.querySelector('video, audio');" +
                            "  if (!v) return 'no media element found';" +
                            "  v.volume = Math.min(1.0, Math.max(0.0, v.volume + delta));" +
                            "  return 'volume=' + Math.round(v.volume * 100) + '%';" +
                            "}", List.of(delta));
                    return new ExecutionResult(true, String.valueOf(volRes));
                }

                if ("seek".equals(cmd)) {
                    int sec = action.getSeekSeconds() != null ? action.getSeekSeconds() : 10;
                    Object seekRes = page.evaluate("([sec]) => {" +
                            "  const v = document.querySelector('video, audio');" +
                            "  if (!v) return 'no media element found';" +
                            "  v.currentTime = Math.max(0, v.currentTime + sec);" +
                            "  return 'currentTime=' + Math.round(v.currentTime) + 's';" +
                            "}", List.of(sec));
                    return new ExecutionResult(true, String.valueOf(seekRes));
                }

                Object res = page.evaluate("() => {" +
                        "  const v = document.querySelector('video, audio');" +
                        "  if (!v) return 'no media found';" +
                        "  if (v.paused) { v.play(); return 'playing'; } else { v.pause(); return 'paused'; }" +
                        "}");
                return new ExecutionResult(true, String.valueOf(res));
            }

            case "press_key": {
                String key = action.getKeyName() != null ? action.getKeyName() : "Enter";
                browser.overlay("toast", "⌨ key " + key);
                page.keyboard().press(key);
                settle(page, 500);
                return new ExecutionResult(true, "pressed " + key);
            }

            case "scroll_top": {
                browser.overlay("toast", "⬆ scroll to top");
                page.evaluate("() => window.scrollTo({ top: 0, behavior: 'smooth' })");
                try { Thread.sleep(300); } catch (Exception ignored) {}
                return new ExecutionResult(true, "scrollY=0");
            }

            case "scroll_bottom": {
                browser.overlay("toast", "⬇ scroll to bottom");
                page.evaluate("() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })");
                try { Thread.sleep(300); } catch (Exception ignored) {}
                return new ExecutionResult(true, "scrollY=bottom");
            }

            case "clear_field": {
                browser.overlay("toast", "⌫ clear field");
                if (action.getTargetId() != null) {
                    Locator loc = locatorFor(page, action.getTargetId());
                    try { loc.fill(""); } catch (Exception ignored) {}
                } else {
                    page.keyboard().press("Control+A");
                    page.keyboard().press("Backspace");
                }
                return new ExecutionResult(true, "field cleared");
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
