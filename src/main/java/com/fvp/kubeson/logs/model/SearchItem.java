package com.fvp.kubeson.logs.model;

public class SearchItem {

    private int index;

    private int start;

    private int end;

    public SearchItem(int index, int start, int end) {
        this.index = index;
        this.start = start;
        this.end = end;
    }

    public int getIndex() {
        return index;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
