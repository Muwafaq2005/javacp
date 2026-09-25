package ai.typesafe.voicebrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElementSnapshot {
    private String id;
    private String tag;
    private String role;
    private String text;
    private String placeholder;
    private String href;
    private String type;
    private String inputName;
    private Boolean inViewport;
    @JsonProperty("below_fold")
    private Boolean belowFold;
    private Integer top;
    private Integer left;

    public ElementSnapshot() {}

    public ElementSnapshot(String id, String role, String text) {
        this.id = id;
        this.role = role;
        this.text = text;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getInputName() { return inputName; }
    public void setInputName(String inputName) { this.inputName = inputName; }

    public Boolean getInViewport() { return inViewport; }
    public void setInViewport(Boolean inViewport) { this.inViewport = inViewport; }

    public Boolean getBelowFold() { return belowFold; }
    public void setBelowFold(Boolean belowFold) { this.belowFold = belowFold; }

    public Integer getTop() { return top; }
    public void setTop(Integer top) { this.top = top; }

    public Integer getLeft() { return left; }
    public void setLeft(Integer left) { this.left = left; }
}
