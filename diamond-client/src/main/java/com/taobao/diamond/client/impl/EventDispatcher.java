package com.taobao.diamond.client.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.taobao.diamond.client.impl.DiamondEnv.log;


public class EventDispatcher {

    static public void addEventListener(EventListener listener) {
        for (Class<? extends Event> type : listener.interest()) {
            getListenerList(type).addIfAbsent(listener);
        }
    }

    static public void fireEvent(Event event) {
        if (null == event) { // ±£»¤
            return;
        }

        for (Event implyEvent : event.implyEvents()) {
            try {
                if (event != implyEvent) { 
                    fireEvent(implyEvent);
                }
            } catch (Exception e) {
                log.warn("", e.toString(), e);
            }
        }

        for (EventListener listener : getListenerList(event.getClass())) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn(e.toString(), e);
            }
        }
    }

    static synchronized CopyOnWriteArrayList<EventListener> getListenerList(
            Class<? extends Event> eventType) {
        CopyOnWriteArrayList<EventListener> listeners = listenerMap.get(eventType);
        if (null == listeners) {
            listeners = new CopyOnWriteArrayList<EventListener>();
            listenerMap.put(eventType, listeners);
        }
        return listeners;
    }

    // ========================
    
    static final Map<Class<? extends Event>, CopyOnWriteArrayList<EventListener>> listenerMap //
    = new HashMap<Class<? extends Event>, CopyOnWriteArrayList<EventListener>>();

    // ========================

    static public abstract class Event {
        @SuppressWarnings("unchecked")
        protected List<Event> implyEvents() {
            return Collections.EMPTY_LIST;
        }
    }

    static public abstract class EventListener {
        public EventListener() {
            EventDispatcher.addEventListener(this); 
        }
        
        abstract public List<Class<? extends Event>> interest();

        abstract public void onEvent(Event event);
    }
    
    static public class ServerlistChangeEvent extends Event {}
}