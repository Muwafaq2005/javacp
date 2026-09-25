package ai.typesafe.voicebrowser.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Action {
    private String type;
    private String targetId;
    private String text;
    private String url;
    private String query;
    private String amount;
    private String direction;
    private String label;
    private Boolean submit;
    private Boolean confirmed;

    public Action() {}

    public Action(String type, String label) {
        this.type = type;
        this.label = label;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Boolean getSubmit() { return submit; }
    public void setSubmit(Boolean submit) { this.submit = submit; }

    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
}
