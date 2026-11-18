package com.machly.app.models;

import java.util.Collections;
import java.util.List;

public class EquipmentPage {
    private List<Equipment> items;
    private int page;
    private int limit;
    private int total;
    private int pages;

    public List<Equipment> getItems() { return items == null ? Collections.emptyList() : items; }
    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public int getTotal() { return total; }
    public int getPages() { return pages; }
}
