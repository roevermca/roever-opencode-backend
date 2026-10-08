package com.ams.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkDeleteRequest {

    private List<String> ids = new ArrayList<>();

    public BulkDeleteRequest() {
    }

    public BulkDeleteRequest(List<String> ids) {
        this.ids = ids != null ? ids : new ArrayList<>();
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids != null ? ids : new ArrayList<>();
    }
}
