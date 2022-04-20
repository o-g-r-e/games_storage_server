package com.cm.dataserver.template1classes.eventsclasses;

import com.cm.dataserver.helpers.JSONConverter;

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

    public String toJson() {
        return String.format("{'%s':'%s','%s':'%s','%s':'%s'}", "end", endDate, "rewardName", reward.getName(), "rewardCount", reward.getCount());
    }
}