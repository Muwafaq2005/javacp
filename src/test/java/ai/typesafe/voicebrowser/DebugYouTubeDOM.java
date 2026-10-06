package ai.typesafe.voicebrowser;

import com.microsoft.playwright.*;
import java.util.*;

public class DebugYouTubeDOM {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            System.out.println("1. Navigating to YouTube search for 'nick digiovanni'...");
            page.navigate("https://www.youtube.com/results?search_query=nick+digiovanni");
            try { Thread.sleep(4000); } catch (Exception ignored) {}

            Object evalResult = page.evaluate("""
                (([targetIdx, cat]) => {
                  let sel = 'a[href*="/watch?v="], ytd-video-renderer a#video-title, ytd-rich-item-renderer a#video-title';
                  const rawItems = Array.from(document.querySelectorAll(sel));
                  const items = rawItems.filter(el => {
                    const r = el.getBoundingClientRect();
                    return r.width > 0 && r.height > 0 && window.getComputedStyle(el).visibility !== 'hidden';
                  });
                  return items.map((el, i) => ({
                    index: i,
                    href: el.getAttribute('href') || '',
                    title: el.getAttribute('title') || el.innerText || '',
                    tag: el.tagName,
                    class: el.className,
                    parent: el.parentElement ? el.parentElement.tagName + '.' + el.parentElement.className : ''
                  }));
                })([2, 'video'])
            """);

            System.out.println("Raw result: " + evalResult);

            System.out.println("\n2. Navigating to a video page to check recommendations DOM...");
            page.navigate("https://www.youtube.com/watch?v=vBmq454J4p8");
            try { Thread.sleep(4000); } catch (Exception ignored) {}

            Object recResult = page.evaluate("""
                (() => {
                  let sel = 'a[href*="/watch?v="], ytd-compact-video-renderer a#thumbnail, ytd-compact-video-renderer a';
                  const rawItems = Array.from(document.querySelectorAll(sel));
                  const items = rawItems.filter(el => {
                    const r = el.getBoundingClientRect();
                    return r.width > 0 && r.height > 0 && window.getComputedStyle(el).visibility !== 'hidden';
                  });
                  return items.map((el, i) => ({
                    index: i,
                    href: el.getAttribute('href') || '',
                    title: el.getAttribute('title') || el.innerText || '',
                    tag: el.tagName,
                    parent: el.parentElement ? el.parentElement.tagName + '.' + el.parentElement.className : ''
                  }));
                })()
            """);

            System.out.println("Rec result: " + recResult);

            browser.close();
        }
    }
}
