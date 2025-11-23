package com.monframework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view; // ex : "page.jsp"
    private Map<String, Object> data = new HashMap<>();

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addItem(String key, Object value) {
        data.put(key, value);
    }
}
