package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PolicyEngine {

    public record ChoiceProbability(String id, double p) {}

    public static List<ChoiceProbability> topChoices(Map<String, Object> choiceAnswer, int n) {
        if (choiceAnswer == null || !choiceAnswer.containsKey("probabilities")) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> probs = (Map<String, Object>) choiceAnswer.get("probabilities");
        List<ChoiceProbability> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : probs.entrySet()) {
            if ("none".equals(entry.getKey())) continue;
            double p = ((Number) entry.getValue()).doubleValue();
            list.add(new ChoiceProbability(entry.getKey(), Math.round(p * 100.0) / 100.0));
        }
        list.sort((a, b) -> Double.compare(b.p(), a.p()));
        if (list.size() > n) return list.subList(0, n);
        return list;
    }

    private static String pickSpan(Map<String, Object> answer, double minConfidence, String fallback) {
        if (answer == null) return fallback;
        String choice = (String) answer.get("choice");
        if ("none".equals(choice)) return null;
        double conf = answer.containsKey("confidence") ? ((Number) answer.get("confidence")).doubleValue() : 0.0;
        if (conf < minConfidence) return fallback != null ? fallback : choice;
        return choice;
    }

    private static String fillTemplate(String tpl, String q) {
        try {
            return String.format(tpl, java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return tpl.replace("%s", q);
        }
    }

    public static PolicyResult evaluatePolicy(
            Map<String, Map<String, Object>> answers,
            Map<String, List<String>> candidates,
            List<ElementSnapshot> elements,
            String pageUrl,
            String pageTitle,
            String site,
            String searchBoxId,
            long silentMs,
            boolean isFinal,
            Action pending,
            List<ActionHistoryEntry> recentActions
    ) {
        List<PolicyResult.GateReason> reasons = new ArrayList<>();

        Map<String, Object> intentObj = answers.get("intent");
        String intentName = intentObj != null && intentObj.containsKey("choice") ? (String) intentObj.get("choice") : "none";
        double intentConf = intentObj != null && intentObj.containsKey("confidence") ? ((Number) intentObj.get("confidence")).doubleValue() : 0.0;

        List<ActionHistoryEntry> safeActions = (recentActions != null && !recentActions.isEmpty()) ? new ArrayList<>(recentActions) : Collections.emptyList();
        ActionHistoryEntry lastAction = !safeActions.isEmpty() ? safeActions.get(safeActions.size() - 1) : null;
        double correction = answers.containsKey("is_correction") && answers.get("is_correction").containsKey("noul") ?
                ((Number) answers.get("is_correction").get("noul")).doubleValue() : 0.0;

        double completeNoul = answers.containsKey("complete") && answers.get("complete").containsKey("noul") ?
                ((Number) answers.get("complete").get("noul")).doubleValue() : 0.0;

        boolean finishedPhrase = completeNoul >= Constants.T.complete() || silentMs >= Constants.SILENCE_COMPLETE_MS || isFinal;
        boolean isCorrection = (lastAction != null) && correction >= Constants.T.correction() && finishedPhrase;

        // 0. Pending confirmation/cancellation
        if (pending != null) {
            if ("confirm".equals(intentName) && intentConf >= Constants.T.intentConfidence()) {
                reasons.add(new PolicyResult.GateReason("intent", "confirm (" + Math.round(intentConf * 100.0) / 100.0 + ")", Constants.T.intentConfidence(), true, "pending action confirmed"));
                Action confirmedAction = pending;
                confirmedAction.setConfirmed(true);
                PolicyResult res = new PolicyResult("act", "confirmed: " + describe(pending));
                res.setAction(confirmedAction);
                res.setReasons(reasons);
                return res;
            }
            if ("cancel".equals(intentName) && intentConf >= Constants.T.intentConfidence()) {
                reasons.add(new PolicyResult.GateReason("intent", "cancel (" + Math.round(intentConf * 100.0) / 100.0 + ")", Constants.T.intentConfidence(), true, "pending action cancelled"));
                PolicyResult res = new PolicyResult("cancel", "cancelled pending action");
                res.setReasons(reasons);
                return res;
            }
        }

        // 1. Correction handling
        if (isCorrection) {
            reasons.add(new PolicyResult.GateReason("is_correction", correction, Constants.T.correction(), true, "rejects previous action: " + lastAction.said()));
            boolean confidentIntent = !"none".equals(intentName) && intentConf >= Constants.T.intentConfidence();
            boolean namesNewTarget = Constants.TARGET_INTENTS.contains(intentName) && topChoices(answers.get("target"), 2).stream()
                    .anyMatch(c -> !c.id().equals(lastAction.targetId()) && c.p() >= Constants.T.targetTopProb());

            boolean reverse = !"go_back".equals(lastAction.type()) && (!confidentIntent || (Constants.TARGET_INTENTS.contains(intentName) && !namesNewTarget));
            if (reverse) {
                Action reversal = reverseAction(lastAction);
                PolicyResult res = new PolicyResult("act", "correction → " + describe(reversal));
                res.setAction(reversal);
                res.setReasons(reasons);
                return res;
            }
        }

        // 1b. Is command gate
        double isCmd = answers.containsKey("is_command") && answers.get("is_command").containsKey("noul") ?
                ((Number) answers.get("is_command").get("noul")).doubleValue() : 0.0;
        boolean isCmdPass = isCmd >= Constants.T.isCommand();
        reasons.add(new PolicyResult.GateReason("is_command", Math.round(isCmd * 100.0) / 100.0, Constants.T.isCommand(), isCmdPass, "user is addressing the browser"));
        if (!isCmdPass) {
            PolicyResult res = new PolicyResult("ignore", "not a browser command");
            res.setReasons(reasons);
            return res;
        }

        // 2. Intent confidence gate
        boolean intentOk = !"none".equals(intentName) && intentConf >= Constants.T.intentConfidence();
        reasons.add(new PolicyResult.GateReason("intent", intentName + " (" + Math.round(intentConf * 100.0) / 100.0 + ")", Constants.T.intentConfidence(), intentOk, "confident, non-none intent"));
        if (!intentOk) {
            PolicyResult res = new PolicyResult("wait", "none".equals(intentName) ? "no recognizable command yet" : "intent not confident yet");
            res.setReasons(reasons);
            return res;
        }

        // 3. Completeness gate
        boolean silent = silentMs >= Constants.SILENCE_COMPLETE_MS || isFinal;
        boolean completeOk = completeNoul >= Constants.T.complete() || silent;
        reasons.add(new PolicyResult.GateReason("complete", completeNoul, Constants.T.complete(), completeOk, silent ? (isFinal ? "recognizer marked utterance final" : "silent for " + silentMs + "ms") : "command has verb + object"));
        if (!completeOk) {
            PolicyResult res = new PolicyResult("wait", "waiting for the rest of the command");
            res.setReasons(reasons);
            return res;
        }

        // 3b. Free text payload gate
        if (Constants.PAYLOAD_INTENTS.contains(intentName)) {
            boolean payloadOk = isFinal || silentMs >= Constants.PAYLOAD_SILENCE_MS;
            reasons.add(new PolicyResult.GateReason("payload_final", isFinal ? "final" : silentMs + "ms silence", "final or " + Constants.PAYLOAD_SILENCE_MS + "ms", payloadOk, "free text must be finished"));
            if (!payloadOk) {
                PolicyResult res = new PolicyResult("wait", "waiting for the end of the phrase (free text)");
                res.setRetryInMs(Math.max(50, Constants.PAYLOAD_SILENCE_MS - silentMs));
                res.setReasons(reasons);
                return res;
            }
        }

        // 4. Build action
        String excludeTargetId = (isCorrection && Constants.TARGET_INTENTS.contains(intentName) && lastAction != null) ? lastAction.targetId() : null;
        PolicyResult built = buildAction(intentName, answers, candidates, elements, pageUrl, pageTitle, site, searchBoxId, excludeTargetId, reasons);
        if (!"act".equals(built.getDecision())) {
            built.setReasons(reasons);
            return built;
        }
        Action action = built.getAction();

        // 5. Destructive gate
        double destructive = answers.containsKey("destructive") && answers.get("destructive").containsKey("noul") ?
                ((Number) answers.get("destructive").get("noul")).doubleValue() : 0.0;
        boolean canBeDestructive = Set.of("click_element", "press_enter", "select_option").contains(action.getType());
        if (canBeDestructive) {
            boolean safe = destructive < Constants.T.destructive();
            reasons.add(new PolicyResult.GateReason("destructive", destructive, Constants.T.destructive(), safe, safe ? "reversible action" : "needs spoken confirmation"));
            if (!safe) {
                PolicyResult res = new PolicyResult("confirm", "say \"confirm\" to " + describe(action));
                res.setAction(action);
                res.setReasons(reasons);
                return res;
            }
        }

        PolicyResult res = new PolicyResult("act", describe(action));
        res.setAction(action);
        res.setReasons(reasons);
        return res;
    }

    public record ActionHistoryEntry(String type, String targetId, String targetLabel, String text, String url, String said, boolean ok, String outcome, long at) {}

    public static Action reverseAction(ActionHistoryEntry action) {
        if (action == null) return new Action("go_back", "undo");
        switch (action.type()) {
            case "type_into_field":
                Action a = new Action("type_into_field", "clear " + (action.targetLabel() != null ? action.targetLabel() : action.targetId()));
                a.setTargetId(action.targetId());
                a.setText("");
                a.setSubmit(false);
                return a;
            case "open_new_tab":
                return new Action("close_tab", "close the new tab");
            case "close_tab":
                return new Action("go_back", "back (tab already closed)");
            case "switch_tab":
                Action sw = new Action("switch_tab", "switch back");
                sw.setDirection("previous");
                return sw;
            case "scroll_down":
                Action su = new Action("scroll_up", "scroll back up");
                su.setAmount("page");
                return su;
            case "scroll_up":
                Action sd = new Action("scroll_down", "scroll back down");
                sd.setAmount("page");
                return sd;
            default:
                return new Action("go_back", "undo " + action.said());
        }
    }

    private static PolicyResult buildAction(
            String intentName,
            Map<String, Map<String, Object>> answers,
            Map<String, List<String>> candidates,
            List<ElementSnapshot> elements,
            String pageUrl,
            String pageTitle,
            String currentSite,
            String searchBoxId,
            String excludeTargetId,
            List<PolicyResult.GateReason> reasons
    ) {
        String site = answers.containsKey("site") && answers.get("site").containsKey("choice") ? (String) answers.get("site").get("choice") : "none";
        List<String> textCandidates = candidates != null ? candidates.get("text") : Collections.emptyList();
        List<String> urlCandidates = candidates != null ? candidates.get("url") : Collections.emptyList();

        switch (intentName) {
            case "navigate_url": {
                String urlPick = pickSpan(answers.get("url_span"), Constants.T.spanConfidence(), !urlCandidates.isEmpty() ? urlCandidates.get(0) : null);
                if (urlPick != null) {
                    reasons.add(new PolicyResult.GateReason("url_span", urlPick, Constants.T.spanConfidence(), true, "domain spoken verbatim"));
                    Action act = new Action("navigate_url", urlPick);
                    act.setUrl(SpansExtractor.toHttpUrl(urlPick));
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }
                if (Constants.SITE_HOME.containsKey(site)) {
                    reasons.add(new PolicyResult.GateReason("site", site, "-", true, "known site"));
                    Action act = new Action("navigate_url", site);
                    act.setUrl(Constants.SITE_HOME.get(site));
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }
                return new PolicyResult("wait", "where to? (no site or domain recognised)");
            }

            case "search_web": {
                String query = pickSpan(answers.get("text_span"), Constants.T.spanConfidence(), !textCandidates.isEmpty() ? textCandidates.get(0) : null);
                if (query == null) {
                    return new PolicyResult("wait", "search for what?");
                }
                reasons.add(new PolicyResult.GateReason("text_span", query, Constants.T.spanConfidence(), true, "query copied verbatim"));
                if (Constants.SITE_SEARCH.containsKey(site)) {
                    Action act = new Action("navigate_url", "search " + site + ": " + query);
                    act.setUrl(fillTemplate(Constants.SITE_SEARCH.get(site), query));
                    act.setQuery(query);
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }
                if (searchBoxId != null && !"blank".equals(currentSite)) {
                    Action act = new Action("type_into_field", "search this site: " + query);
                    act.setTargetId(searchBoxId);
                    act.setText(query);
                    act.setSubmit(true);
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }
                Action act = new Action("navigate_url", "search: " + query);
                act.setUrl(fillTemplate(Constants.SITE_SEARCH.get(Constants.DEFAULT_SEARCH_ENGINE), query));
                act.setQuery(query);
                PolicyResult res = new PolicyResult("act", describe(act));
                res.setAction(act);
                return res;
            }

            case "click_element":
            case "select_option":
            case "type_into_field": {
                Map<String, Object> targetMap = answers.get("target");
                List<ChoiceProbability> top = topChoices(targetMap, Constants.T.candidateCount() + 1);
                String chosen = targetMap != null && targetMap.containsKey("choice") ? (String) targetMap.get("choice") : "none";
                double targetConf = targetMap != null && targetMap.containsKey("confidence") ? ((Number) targetMap.get("confidence")).doubleValue() : 0.0;

                if (excludeTargetId != null && excludeTargetId.equals(chosen)) {
                    top = top.stream().filter(c -> !c.id().equals(excludeTargetId)).collect(Collectors.toList());
                    chosen = !top.isEmpty() ? top.get(0).id() : "none";
                }
                if (top.size() > Constants.T.candidateCount()) {
                    top = top.subList(0, Constants.T.candidateCount());
                }

                double chosenP = 0.0;
                if (targetMap != null && targetMap.containsKey("probabilities")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> probs = (Map<String, Object>) targetMap.get("probabilities");
                    if (probs.containsKey(chosen)) {
                        chosenP = ((Number) probs.get(chosen)).doubleValue();
                    }
                }

                boolean targetOk = chosen != null && !"none".equals(chosen) && targetConf >= Constants.T.targetConfidence() && chosenP >= Constants.T.targetTopProb();
                String text = "click_element".equals(intentName) ? null : pickSpan(answers.get("text_span"), Constants.T.spanConfidence(), !textCandidates.isEmpty() ? textCandidates.get(0) : null);

                if (!"click_element".equals(intentName) && text == null) {
                    return new PolicyResult("wait", "type what?");
                }

                if (targetOk) {
                    reasons.add(new PolicyResult.GateReason("target", chosen + " (" + Math.round(targetConf * 100.0) / 100.0 + ")", Constants.T.targetConfidence(), true, elementLabel(elements, chosen)));
                    Action act = new Action(intentName, elementLabel(elements, chosen));
                    act.setTargetId(chosen);
                    act.setText(text);
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }

                if ("type_into_field".equals(intentName) && searchBoxId != null) {
                    Action act = new Action(intentName, "search box");
                    act.setTargetId(searchBoxId);
                    act.setText(text);
                    PolicyResult res = new PolicyResult("act", describe(act));
                    res.setAction(act);
                    return res;
                }

                List<PolicyResult.CandidateChoice> viable = new ArrayList<>();
                for (ChoiceProbability c : top) {
                    if (c.p() >= 0.08) {
                        viable.add(new PolicyResult.CandidateChoice(c.id(), elementLabel(elements, c.id()), c.p()));
                    }
                }
                if (viable.isEmpty()) {
                    return new PolicyResult("wait", "no matching element on this page");
                }
                PolicyResult res = new PolicyResult("disambiguate", "which one?");
                res.setCandidates(viable);
                Action pendingIntent = new Action(intentName, intentName);
                pendingIntent.setText(text);
                res.setPendingIntent(pendingIntent);
                return res;
            }

            case "scroll_down":
            case "scroll_up": {
                Map<String, Object> scrollObj = answers.get("scroll_amount");
                double score = scrollObj != null && scrollObj.containsKey("score") ? ((Number) scrollObj.get("score")).doubleValue() : 1.0;
                int lvl = (int) Math.round(score);
                String amount = List.of("little", "page", "end").get(Math.min(2, Math.max(0, lvl)));
                Action act = new Action(intentName, intentName.replace("_", " ") + " (" + amount + ")");
                act.setAmount(amount);
                PolicyResult res = new PolicyResult("act", describe(act));
                res.setAction(act);
                return res;
            }

            case "switch_tab": {
                Map<String, Object> tabObj = answers.get("tab_direction");
                String dir = tabObj != null && tabObj.containsKey("choice") && !"none".equals(tabObj.get("choice")) ? (String) tabObj.get("choice") : "next";
                Action act = new Action("switch_tab", "switch tab (" + dir + ")");
                act.setDirection(dir);
                PolicyResult res = new PolicyResult("act", describe(act));
                res.setAction(act);
                return res;
            }

            default:
                Action act = new Action(intentName, intentName.replace("_", " "));
                PolicyResult res = new PolicyResult("act", describe(act));
                res.setAction(act);
                return res;
        }
    }

    public static String elementLabel(List<ElementSnapshot> elements, String id) {
        if (elements == null) return id;
        for (ElementSnapshot el : elements) {
            if (id.equals(el.getId())) {
                String label = el.getText() != null && !el.getText().isEmpty() ? el.getText() : (el.getPlaceholder() != null ? el.getPlaceholder() : "");
                return el.getRole() + " \"" + label + "\"";
            }
        }
        return id;
    }

    public static String describe(Action action) {
        if (action == null) return "";
        switch (action.getType()) {
            case "navigate_url":
                return "open " + (action.getLabel() != null ? action.getLabel() : action.getUrl());
            case "type_into_field":
                return "type \"" + action.getText() + "\" into " + (action.getLabel() != null ? action.getLabel() : action.getTargetId()) + (Boolean.TRUE.equals(action.getSubmit()) ? " + enter" : "");
            case "click_element":
                return "click " + (action.getLabel() != null ? action.getLabel() : action.getTargetId());
            case "select_option":
                return "select \"" + action.getText() + "\" in " + (action.getLabel() != null ? action.getLabel() : action.getTargetId());
            default:
                return action.getLabel() != null ? action.getLabel() : action.getType().replace('_', ' ');
        }
    }
}
