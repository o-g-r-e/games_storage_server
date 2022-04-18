package com.cm.dataserver.template1classes.eventsclasses;

public class Event {
    private String endDate;
    private Reward reward;
    
    public Event(String endDate, Reward reward) {
        this.endDate = endDate;
        this.reward = reward;
    }

    public String getEndDate() {
        return endDate;
    }

    public Reward getReward() {
        return reward;
    }
}
