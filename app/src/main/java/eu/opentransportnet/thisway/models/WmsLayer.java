package eu.opentransportnet.thisway.models;

/**
 * @author Kristaps Krumins
 */
public class WmsLayer {

    String name = null;
    String title = null;
    String wmsUrl = null;
    boolean selected = false;

    public WmsLayer(String name, String title, String wmsUrl, boolean selected) {
        super();
        this.name = name;
        this.title = title;
        this.wmsUrl = wmsUrl;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getWmsUrl() {
        return wmsUrl;
    }

    public void setWmsUrl(String url) {
        this.wmsUrl = url;
    }

}