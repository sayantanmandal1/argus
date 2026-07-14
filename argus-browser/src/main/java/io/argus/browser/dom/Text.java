package io.argus.browser.dom;

/** A text node holding character data. */
public final class Text extends Node {

    private String data;

    public Text(String data) {
        this.data = data == null ? "" : data;
    }

    public String data() {
        return data;
    }

    public void setData(String data) {
        this.data = data == null ? "" : data;
    }

    @Override
    public String nodeName() {
        return "#text";
    }

    @Override
    void collectText(StringBuilder sb) {
        sb.append(data);
    }

    @Override
    public String toString() {
        return "\"" + data + "\"";
    }
}
