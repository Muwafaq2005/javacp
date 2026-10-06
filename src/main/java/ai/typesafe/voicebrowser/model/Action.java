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

    private String keyName;
    private String mediaCommand;
    private Integer tabIndex;
    private Integer ordinalIndex;
    private String targetCategory;
    private Double volumeLevel;
    private Integer seekSeconds;

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public String getMediaCommand() { return mediaCommand; }
    public void setMediaCommand(String mediaCommand) { this.mediaCommand = mediaCommand; }

    public Integer getTabIndex() { return tabIndex; }
    public void setTabIndex(Integer tabIndex) { this.tabIndex = tabIndex; }

    public Integer getOrdinalIndex() { return ordinalIndex; }
    public void setOrdinalIndex(Integer ordinalIndex) { this.ordinalIndex = ordinalIndex; }

    public String getTargetCategory() { return targetCategory; }
    public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }

    public Double getVolumeLevel() { return volumeLevel; }
    public void setVolumeLevel(Double volumeLevel) { this.volumeLevel = volumeLevel; }

    public Integer getSeekSeconds() { return seekSeconds; }
    public void setSeekSeconds(Integer seekSeconds) { this.seekSeconds = seekSeconds; }

    private Double playbackRate;
    private String ytCommand;

    public Double getPlaybackRate() { return playbackRate; }
    public void setPlaybackRate(Double playbackRate) { this.playbackRate = playbackRate; }

    public String getYtCommand() { return ytCommand; }
    public void setYtCommand(String ytCommand) { this.ytCommand = ytCommand; }
}
