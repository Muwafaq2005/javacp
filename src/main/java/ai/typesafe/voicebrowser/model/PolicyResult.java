package ai.typesafe.voicebrowser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResult {
    private String decision; // act, wait, ignore, confirm, disambiguate, cancel
    private Action action;
    private List<CandidateChoice> candidates;
    private Action pendingIntent;
    private List<GateReason> reasons;
    private String summary;
    private Long retryInMs;

    public record CandidateChoice(String id, String label, double p) {}
    public record GateReason(String name, Object value, Object threshold, boolean pass, String note) {}

    public PolicyResult() {}

    public PolicyResult(String decision, String summary) {
        this.decision = decision;
        this.summary = summary;
    }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public List<CandidateChoice> getCandidates() { return candidates; }
    public void setCandidates(List<CandidateChoice> candidates) { this.candidates = candidates; }

    public Action getPendingIntent() { return pendingIntent; }
    public void setPendingIntent(Action pendingIntent) { this.pendingIntent = pendingIntent; }

    public List<GateReason> getReasons() { return reasons; }
    public void setReasons(List<GateReason> reasons) { this.reasons = reasons; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Long getRetryInMs() { return retryInMs; }
    public void setRetryInMs(Long retryInMs) { this.retryInMs = retryInMs; }
}
