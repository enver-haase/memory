package com.example.application.profiling;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.server.VaadinSession;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.application.profiling.ObjDescription.getDescription;

public class VaadinVisitor {

    Logger logger = Logger.getLogger(VaadinVisitor.class.getName());

    public interface VaadinStatistics{
        int getNumberOfVaadinSessions();
        Map<String, Set<String>> getUI2ComponentsMap();
        Map<String, Set<String>> getSession2UIsMap();
        Map<String, Set<String>> getSession2AttribKeysMap();

        Set<String> getAttributesOfCurrentSession();
        Set<String> getUIsOfCurrentSession();
        Set<String> getComponentsOfCurrentUI();
        Set<String> getOrphanedComponents();
    }

    public VaadinStatistics getVaadinStatistics(){
        return new VaadinStatistics() {
            @Override
            public int getNumberOfVaadinSessions() {
                return VaadinVisitor.this.numVaadinSessions;
            }

            @Override
            public Map<String, Set<String>> getUI2ComponentsMap() {
                // even better: make the sets in the map unmodifiable too, but that's for another time
                return Collections.unmodifiableMap(VaadinVisitor.this.components);
            }

            @Override
            public Map<String, Set<String>> getSession2UIsMap() {
                // even better: make the sets in the map unmodifiable too, but that's for another time
                return Collections.unmodifiableMap(VaadinVisitor.this.uis);
            }

            @Override
            public Map<String, Set<String>> getSession2AttribKeysMap() {
                return Collections.unmodifiableMap(VaadinVisitor.this.attributes);
            }

            @Override
            public Set<String> getAttributesOfCurrentSession() {
                VaadinSession currentSession = VaadinSession.getCurrent();
                if (currentSession == null){
                    throw new RuntimeException("Called out of a Vaadin execution context!");
                }
                else{
                    return Collections.unmodifiableSet(attributes.get(getDescription(currentSession)));
                }
            }

            @Override
            public Set<String> getUIsOfCurrentSession() {
                VaadinSession currentSession = VaadinSession.getCurrent();
                if (currentSession == null){
                    throw new RuntimeException("Called out of a Vaadin execution context!");
                }
                else{
                    return Collections.unmodifiableSet(uis.get(getDescription(currentSession)));
                }
            }

            @Override
            public Set<String> getComponentsOfCurrentUI() {
                UI currentUI = UI.getCurrent();
                if (currentUI == null){
                    throw new RuntimeException("Called out of a Vaadin execution context!");
                }
                else{
                    return Collections.unmodifiableSet(components.get(getDescription(currentUI)));
                }
            }

            @Override
            public Set<String> getOrphanedComponents() {
                Set<String> orphans = VaadinVisitor.this.components.get(getDescription(null));
                return (orphans == null? Collections.emptySet() : Collections.unmodifiableSet(orphans));
            }
        };
    }

    private final HashMap<String, Set<String>> components; // ui->components
    private final HashMap<String, Set<String>> uis; // session->uis
    private final HashMap<String, Set<String>> attributes; // session->attribute-keys
    private int numVaadinSessions;

    public VaadinVisitor(){
        this.components = new HashMap<>();
        this.uis = new HashMap<>();
        this.attributes = new HashMap<>();
        numVaadinSessions = 0;
    }

    public void accept(Object object) {
        if (object instanceof VaadinSession){
            numVaadinSessions++;

            VaadinSession vaadinSession = (VaadinSession) object;
            try {
                Class clazz = vaadinSession.getClass();
                while (!clazz.getName().equals("com.vaadin.flow.server.VaadinSession")){
                    clazz = clazz.getSuperclass(); // e.g. in a Spring Boot application the class at hand is only _derived_ from VaadinSession
                }
                Field attribsField = clazz.getDeclaredField("attributes");
                attribsField.setAccessible(true);
                Attributes attributes = (Attributes) (attribsField.get(vaadinSession));
                Field hashField = attributes.getClass().getDeclaredField("attributes");
                hashField.setAccessible(true);
                HashMap<String, Object> attribs = (HashMap<String, Object>) (hashField.get(attributes));
                Set<String> keys = Collections.unmodifiableSet(attribs.keySet());
                this.attributes.put(getDescription(vaadinSession), keys);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.log(Level.SEVERE, "Reflection problem.", e);
            }


        }
        if (object instanceof UI){
            UI ui = (UI) object;
            VaadinSession vaadinSession = ui.getSession();
            final String sessionDesc = getDescription(vaadinSession);
            uis.computeIfAbsent(sessionDesc, k -> new HashSet<>());
            uis.get(sessionDesc).add(getDescription(ui));
        }
        else if (object instanceof Component){
            Component component = (Component) object;
            String componentDesc = getDescription(object);

            Optional<UI> optUI = component.getUI();
            UI ui = optUI.orElse(null);
            String uiDesc = getDescription(ui);

            components.computeIfAbsent(uiDesc, k -> new HashSet<>());
            this.components.get(uiDesc).add(componentDesc);
        }
    }
}
