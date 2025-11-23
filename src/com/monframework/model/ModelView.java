package com.monframework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view; // ex : "page.jsp"

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }
}
