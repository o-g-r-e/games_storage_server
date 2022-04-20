package com.cm.dataserver.template1classes.eventsclasses;

public class Reward {
    private String name;
    private int count;
    
    public Reward(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }
}
