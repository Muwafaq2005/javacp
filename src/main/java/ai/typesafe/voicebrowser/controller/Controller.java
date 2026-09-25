package ai.typesafe.voicebrowser.browser;

import ai.typesafe.voicebrowser.model.*;
import ai.typesafe.voicebrowser.service.JevClient;
import ai.typesafe.voicebrowser.service.PolicyEngine;
import ai.typesafe.voicebrowser.service.SnapshotCollector;
import ai.typesafe.voicebrowser.service.SpansExtractor;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Controller {

    private final BrowserManager browser;
    private BrowserManager.SnapshotResult snapshot;
    private long snapshotAt = 0;

    public record Utterance(
            String id,
            String physicalId,
            String prefix,
            int gen,
            String text,
            boolean isFinal,
            long startedAt,
            long updatedAt,
            boolean actedOn,
            String actedText
    ) {}

    public record Consumed(String id, String prefix, int gen) {}
    public record CandidatesOverlay(List<PolicyResult.CandidateChoice> list, Action intent, long at) {}

    private Utterance utterance = null;
    private Consumed consumed = null;
    private Action pending = null;
    private CandidatesOverlay candidates = null;
    private PolicyResult lastDecision = null;

    private boolean busy = false;
    private final List<Map<String, Object>> logEntries = new CopyOnWriteArrayList<>();
    private final Map<String, Object> stats = new ConcurrentHashMap<>();

    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final List<PolicyEngine.ActionHistoryEntry> recentActions = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> debounceTimer;
    private ScheduledFuture<?> silenceTimer;

    private final List<Consumer<Map<String, Object>>> eventListeners = new CopyOnWriteArrayList<>();

    public Controller(BrowserManager browser) {
        this.browser = browser;
        this.context.put("recentActions", recentActions);
        this.stats.put("calls", 0);
        this.stats.put("actions", 0);
        this.stats.put("inputTokens", 0);
        this.stats.put("costUsd", 0.0);
        this.stats.put("model", Constants.MODEL);

        browser.onChange(b -> emit("tabs", browser.getTabInfo()));
    }

    public void onEvent(Consumer<Map<String, Object>> listener) {
        eventListeners.add(listener);
    }

    private void emit(String type, Object payload) {
        Map<String, Object> msg = Map.of("type", type, "payload", payload != null ? payload : Map.of());
        for (Consumer<Map<String, Object>> l : eventListeners) {
            try { l.accept(msg); } catch (Exception ignored) {}
        }
    }

    public synchronized void start() {
        refreshSnapshot();
        log("info", "ready — model " + Constants.MODEL + ", " + (snapshot != null ? snapshot.elements.size() : 0) + " elements on " + (snapshot != null ? snapshot.url : ""));
    }

    public void log(String level, String msg) {
        Map<String, Object> entry = Map.of("t", System.currentTimeMillis(), "level", level, "msg", msg);
        logEntries.add(entry);
        if (logEntries.size() > 200) logEntries.remove(0);
        emit("log", entry);
    }

    public synchronized BrowserManager.SnapshotResult refreshSnapshot() {
        this.snapshot = browser.snapshot();
        this.snapshotAt = System.currentTimeMillis();
        emit("snapshot", uiState().get("snapshot"));
        return this.snapshot;
    }

    public synchronized void handleCommand(String text) {
        handleTranscript(text, true, "typed-" + System.currentTimeMillis());
    }

    public synchronized void handleTranscript(String text, boolean isFinal, String utteranceId) {
        String clean = SpansExtractor.cleanTranscript(text);
        long now = System.currentTimeMillis();

        String virtualId = utteranceId;
        if (consumed != null && consumed.id().equals(utteranceId)) {
            if (!clean.toLowerCase().startsWith(consumed.prefix())) return;
            clean = clean.substring(consumed.prefix().length()).trim();
            if (clean.split("\\s+").length < 2) return;
            virtualId = utteranceId + "+" + consumed.gen();
        }

        if (utterance == null || !utterance.id().equals(virtualId)) {
            utterance = new Utterance(
                    virtualId,
                    utteranceId,
                    consumed != null && consumed.id().equals(utteranceId) ? consumed.prefix() : "",
                    consumed != null && consumed.id().equals(utteranceId) ? consumed.gen() : 0,
                    clean,
                    isFinal,
                    now,
                    now,
                    false,
                    null
            );
        } else {
            if (clean.equals(utterance.text()) && isFinal == utterance.isFinal()) return;
            utterance = new Utterance(
                    utterance.id(),
                    utterance.physicalId(),
                    utterance.prefix(),
                    utterance.gen(),
                    clean,
                    isFinal || utterance.isFinal(),
                    utterance.startedAt(),
                    now,
                    utterance.actedOn(),
                    utterance.actedText()
            );
        }

        emit("transcript", Map.of("text", clean, "final", isFinal, "utteranceId", virtualId, "actedOn", utterance.actedOn()));
        if (clean.isEmpty() || utterance.actedOn()) return;

        // Numbered candidate pick shortcut
        if (candidates != null && now - candidates.at() < Constants.CANDIDATE_TTL_MS) {
            Integer n = SpansExtractor.parseCandidatePick(clean, candidates.list().size());
            if (n != null) {
                PolicyResult.CandidateChoice c = candidates.list().get(n - 1);
                consume(utterance, clean);
                log("info", "picked candidate " + n + " (" + c.label() + ") by number — no model call");
                Action action = new Action(candidates.intent().getType(), c.label());
                action.setTargetId(c.id());
                candidates = null;
                runAction(action, "candidate-pick");
                return;
            }
        }

        if (debounceTimer != null) debounceTimer.cancel(false);
        if (silenceTimer != null) silenceTimer.cancel(false);

        long delay = isFinal ? 0 : Constants.DEBOUNCE_MS;
        debounceTimer = scheduler.schedule(() -> decideNow("debounce"), delay, TimeUnit.MILLISECONDS);
    }

    private synchronized void consume(Utterance utt, String text) {
        utterance = new Utterance(
                utt.id(), utt.physicalId(), utt.prefix(), utt.gen(), utt.text(), utt.isFinal(),
                utt.startedAt(), utt.updatedAt(), true, text
        );
        consumed = new Consumed(utt.physicalId(), (utt.prefix() + " " + text).trim().toLowerCase(), utt.gen() + 1);
    }

    public synchronized void decideNow(String trigger) {
        Utterance utt = utterance;
        if (utt == null || utt.text().isEmpty() || utt.actedOn()) return;
        if (busy) {
            silenceTimer = scheduler.schedule(() -> decideNow("after-action"), 150, TimeUnit.MILLISECONDS);
            return;
        }

        if (System.currentTimeMillis() - snapshotAt > 1500) {
            refreshSnapshot();
        }

        try {
            JevClient.JevDecisionResponse result = JevClient.decide(
                    utt.text(),
                    snapshot != null ? snapshot.elements : Collections.emptyList(),
                    snapshot != null ? snapshot.url : "about:blank",
                    snapshot != null ? snapshot.title : "",
                    snapshot != null ? snapshot.site : "blank",
                    context
            );

            long silentMs = System.currentTimeMillis() - utt.updatedAt();
            PolicyResult policy = PolicyEngine.evaluatePolicy(
                    result.answers(),
                    result.candidates(),
                    snapshot != null ? snapshot.elements : Collections.emptyList(),
                    snapshot != null ? snapshot.url : "about:blank",
                    snapshot != null ? snapshot.title : "",
                    snapshot != null ? snapshot.site : "blank",
                    snapshot != null ? snapshot.searchBoxId : null,
                    silentMs,
                    utt.isFinal(),
                    pending,
                    recentActions
            );

            lastDecision = policy;
            emit("decision", Map.of("transcript", utt.text(), "policy", policy, "latencyMs", result.latencyMs()));
            log("act".equals(policy.getDecision()) ? "act" : "info", result.latencyMs() + "ms · \"" + utt.text() + "\" → " + policy.getDecision() + ": " + policy.getSummary());

            switch (policy.getDecision()) {
                case "act":
                    consume(utt, utt.text());
                    if (policy.getAction() != null && Boolean.TRUE.equals(policy.getAction().getConfirmed())) pending = null;
                    candidates = null;
                    runAction(policy.getAction(), "jev");
                    break;
                case "confirm":
                    consume(utt, utt.text());
                    pending = policy.getAction();
                    browser.overlay("toast", "Say \"confirm\" to " + PolicyEngine.describe(policy.getAction()), 6000);
                    emit("pending", Map.of("action", policy.getAction(), "summary", policy.getSummary()));
                    break;
                case "cancel":
                    consume(utt, utt.text());
                    pending = null;
                    browser.overlay("toast", "cancelled");
                    emit("pending", null);
                    break;
                case "disambiguate":
                    candidates = new CandidatesOverlay(policy.getCandidates(), policy.getPendingIntent(), System.currentTimeMillis());
                    browser.overlay("candidates", policy.getCandidates(), Constants.CANDIDATE_TTL_MS);
                    browser.overlay("toast", "Which one? Say the number.", 3000);
                    emit("candidates", policy.getCandidates());
                    scheduleSilenceRetry(utt, null);
                    break;
                case "wait":
                    scheduleSilenceRetry(utt, policy.getRetryInMs());
                    break;
            }

        } catch (Exception e) {
            log("error", "Jev error: " + e.getMessage());
        }
    }

    private void scheduleSilenceRetry(Utterance utt, Long retryInMs) {
        if (silenceTimer != null) silenceTimer.cancel(false);
        long waitFor = retryInMs != null ? retryInMs : Math.max(50, Constants.SILENCE_COMPLETE_MS - (System.currentTimeMillis() - utt.updatedAt()));
        silenceTimer = scheduler.schedule(() -> {
            if (utterance == utt && !utt.actedOn()) {
                decideNow("silence");
            }
        }, waitFor, TimeUnit.MILLISECONDS);
    }

    private synchronized void runAction(Action action, String via) {
        this.busy = true;
        long t0 = System.currentTimeMillis();
        String pageBeforeUrl = snapshot != null ? snapshot.url : null;
        try {
            Executor.ExecutionResult res = Executor.execute(action, browser);
            long took = System.currentTimeMillis() - t0;
            recordContext(action, res.ok(), res.detail(), utterance != null ? utterance.text() : "", pageBeforeUrl);
            log(res.ok() ? "act" : "warn", (res.ok() ? "✓ " : "✗ ") + PolicyEngine.describe(action) + " — executed in " + took + "ms " + (res.detail() != null ? res.detail() : ""));
            emit("action", Map.of("action", action, "ok", res.ok(), "ui", uiState()));
        } catch (Exception e) {
            log("error", "action failed: " + PolicyEngine.describe(action) + " — " + e.getMessage());
        } finally {
            this.busy = false;
            refreshSnapshot();
        }
    }

    private void recordContext(Action action, boolean ok, String detail, String said, String pageBeforeUrl) {
        String after = browser.currentUrl();
        boolean navigated = pageBeforeUrl != null && after != null && !after.equalsIgnoreCase(pageBeforeUrl);
        if (navigated && snapshot != null) {
            context.put("previousPage", Map.of("url", pageBeforeUrl, "title", snapshot.title != null ? snapshot.title : ""));
        }
        String outcome = ok ? "done" : "failed";
        if (ok && navigated) outcome = "navigated to " + after;
        else if (ok && detail != null && !detail.startsWith("http")) outcome = detail;

        recentActions.add(new PolicyEngine.ActionHistoryEntry(
                action.getType(), action.getTargetId(), action.getLabel(), action.getText(), action.getUrl(),
                said, ok, outcome, System.currentTimeMillis()
        ));
        if (recentActions.size() > Constants.MAX_CONTEXT_ACTIONS) {
            recentActions.remove(0);
        }
    }

    public synchronized void undo() {
        runAction(new Action("go_back", "undo (back)"), "undo");
    }

    public Map<String, Object> uiState() {
        Map<String, Object> map = new HashMap<>();
        map.put("model", Constants.MODEL);
        map.put("stats", stats);
        map.put("snapshot", snapshot);
        map.put("lastDecision", lastDecision);
        map.put("context", context);
        map.put("pending", pending != null ? Map.of("summary", PolicyEngine.describe(pending)) : null);
        map.put("candidates", candidates != null ? candidates.list() : null);
        map.put("log", logEntries);
        return map;
    }

    public void close() {
        if (debounceTimer != null) debounceTimer.cancel(true);
        if (silenceTimer != null) silenceTimer.cancel(true);
        scheduler.shutdown();
    }
}
