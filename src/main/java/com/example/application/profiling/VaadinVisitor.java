package com.example.application.profiling;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import java.util.*;

import static com.example.application.profiling.ObjDescription.getDescription;

public class VaadinVisitor {

    public interface VaadinStatistics{
        int getNumberOfVaadinSessions();
        Map<String, Set<String>> getUI2ComponentsMap();
        Map<String, Set<String>> getSession2UIsMap();

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
    private int numVaadinSessions;

    public VaadinVisitor(){
        this.components = new HashMap<>();
        this.uis = new HashMap<>();
        numVaadinSessions = 0;
    }

    public void accept(Object object) {
        if (object instanceof VaadinSession){
            numVaadinSessions++;
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
