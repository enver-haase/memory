package com.example.application;

import com.example.application.profiling.ObjDescription;
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
            logger.log(Level.WARNING, "Session "+ObjDescription.getDescription(sessionInitEvent.getSession())+" initialized.");
        });
        event.getSource().addSessionDestroyListener( sessionDestroyEvent -> {
            logger.log(Level.WARNING, "Session "+ ObjDescription.getDescription(sessionDestroyEvent.getSession())+" destroyed.");
        });
    }

}
