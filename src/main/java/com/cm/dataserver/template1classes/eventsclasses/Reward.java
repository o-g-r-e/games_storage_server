package com.cm.dataserver.template1classes.eventsclasses;

public class Reward {
    private String eventUuid;
    private String name;
    private int count;
    
    public Reward(String eventUuid, String name, int count) {
        this.eventUuid = eventUuid;
        this.name = name;
        this.count = count;
    }

    public String getEventUuid() {
        return eventUuid;
    }

    public void setEventUuid(String eventUuid) {
        this.eventUuid = eventUuid;
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
        return String.format("{'eventUuid':'%s','rewardName':'%s','count':'%s'}", eventUuid, name, count);
    }
}
