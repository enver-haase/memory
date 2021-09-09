package com.example.application;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationServiceInitListener
        implements VaadinServiceInitListener {

    Logger logger = Logger.getLogger(ApplicationServiceInitListener.class.getName());

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener( sessionInitEvent -> {
            logger.log(Level.WARNING, "Session "+System.identityHashCode(sessionInitEvent.getSession())+" initialized.");
        });
        event.getSource().addSessionDestroyListener( sessionDestroyEvent -> {
            logger.log(Level.WARNING, "Session "+System.identityHashCode(sessionDestroyEvent.getSession())+" destroyed.");
        });
    }

}
