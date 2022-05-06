package com.cm.dataserver.template1classes.eventsclasses;

public class Reward {
    private String eventId;
    private String name;
    private int count;
    
    public Reward(String eventId, String name, int count) {
        this.eventId = eventId;
        this.name = name;
        this.count = count;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String toJson() {
        return String.format("{'rewardId':'%s','rewardName':'%s','count':'%s'}", eventId, name, count);
    }
}
