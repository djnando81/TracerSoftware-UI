package com.tracersoftware.usuarios.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class PagedResult {
    private int page;
    private int size;
    private int totalItems;
    private int totalPages;
    private List<JsonNode> items;

    public PagedResult(int page, int size, int totalItems, int totalPages, List<JsonNode> items) {
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.items = items;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalItems() { return totalItems; }
    public int getTotalPages() { return totalPages; }
    public java.util.List<JsonNode> getItems() { return items; }
}
