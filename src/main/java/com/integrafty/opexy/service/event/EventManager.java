package com.integrafty.opexy.service.event;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EventManager {
    
    private final AtomicBoolean groupEventActive = new AtomicBoolean(false);
    
    public AtomicBoolean getGroupEventActive() {
        return groupEventActive;
    }
    
    private String activeEventName = null;

    public boolean startGroupEvent(String eventName) {
        if (groupEventActive.compareAndSet(false, true)) {
            activeEventName = eventName;
            return true;
        }
        return false;
    }

    public void endGroupEvent() {
        groupEventActive.set(false);
        activeEventName = null;
    }

    public String getActiveEventName() {
        return activeEventName;
    }
}
