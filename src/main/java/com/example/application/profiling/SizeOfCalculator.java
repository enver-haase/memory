package com.example.application.profiling;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import org.ehcache.sizeof.SizeOf;
import org.ehcache.sizeof.VisitorListener;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.application.profiling.ObjDescription.getDescription;

public class SizeOfCalculator {

    private final Object rootRef;
    private final String[] prefixes;
    private final FilteringVisitorListener filteringVisitorListener;

    private final HashMap<String, List<InstanceStatistics>> classnameToInstanceSizes = new HashMap<>();

    private SizeOfCalculator(Object rootRef, String... fqClassnamePrefixes) {
        this.rootRef = rootRef;
        this.prefixes = fqClassnamePrefixes;
        this.filteringVisitorListener = new FilteringVisitorListener();
    }

    Logger logger = Logger.getLogger(SizeOfCalculator.class.getName());

    private final SizeOf sizeOf = SizeOf.newInstance(true, true); // filters can be passed here
    //private final SizeOf sizeOf = new AgentSizeOf(new PassThroughFilter(),true, true); // filters can be passed here
    //private final SizeOf sizeOf = new UnsafeSizeOf(new PassThroughFilter(),true, true); // filters can be passed here
    //private final SizeOf sizeOf = new ReflectionSizeOf(new PassThroughFilter(),true, true); // filters can be passed here

    public interface InstanceStatistics {
        String getDescription();
    }

    public interface ClassStatistics extends Comparable<ClassStatistics> {
        String getClassName();
        InstanceStatistics[] getInstanceStatistics();
    }

    public interface DeepSize {
        long getDeepSize();
        ClassStatistics[] getClassStatistics();
    }

    private static class ClsSize implements ClassStatistics {
        private final String className;
        private final InstanceStatistics[] objectInstanceSizes;

        ClsSize(String className, InstanceStatistics[] objectInstanceSizes) {
            this.className = className;
            this.objectInstanceSizes = objectInstanceSizes;
        }

        @Override
        public String getClassName() {
            return this.className;
        }

        @Override
        public InstanceStatistics[] getInstanceStatistics() {
            return objectInstanceSizes;
        }

        @Override
        public int compareTo(ClassStatistics o) {
            return Long.compare(this.getInstanceStatistics().length, o.getInstanceStatistics().length);
        }
    }

    private static class ObjStatistics implements InstanceStatistics {
        private final String desc;

        ObjStatistics(String description) {
            this.desc = description;
        }

        @Override
        public String getDescription() {
            return desc;
        }
    }


    private class FilteringVisitorListener implements VisitorListener {
        public void visited(Object object, long size) {

            final String className = object.getClass().getName();
            logger.log(Level.FINEST, "Testing an instance of class "+object.getClass().getName());
            for (String prefix : SizeOfCalculator.this.prefixes) {
                if (className.startsWith(prefix)) {
                    // deep size is problematic as the internal BFS also crawls the graph 'upward' from a root object,
                    // as that graph is not a tree in many cases.
                    // This could maybe be fixed by applying a proper "SizeOfFilter" which needs to be experimentally
                    // reached (must be so that e.g. a Vaadin UI would not be crawled upwards to its Vaadin Session,
                    // in order to not read all the other Vaadin UIs, too -- assuming one would want to measure only
                    // the size of one UI.
                    // We leave the size out of the equation for now, and focus on the number of instances we can find instead.
                    InstanceStatistics ois = new ObjStatistics(getDescription(object));
                    List<InstanceStatistics> others = SizeOfCalculator.this.classnameToInstanceSizes.computeIfAbsent(className, k -> new ArrayList<>());
                    others.add(ois);
                    logger.log(Level.FINER, "Found an instance of class "+className);


                    if (object instanceof Component){
                        Component component = (Component) object;
                        Optional<UI> ui = component.getUI();
                        final String uiDesc;
                        uiDesc = ui.map(ObjDescription::getDescription).orElse("(not attached to a UI)");

                        final VaadinSession session;
                        session = ui.map(UI::getSession).orElse(null);
                        String vsDesc = (session == null? "(not attached to a VaadinSession)" : getDescription(session));

                        logger.log(Level.FINE, "Found a Component instance: "+getDescription(object)+", UI: "+uiDesc+", VaadinSession: "+vsDesc);
                    }

                    //break;
                    return; // not break: if we printed the UI as a component, that's sufficient
                }
            }

            if (object instanceof UI){
                UI ui = (UI) object;
                final VaadinSession session;
                session = ui.getSession();
                String vsDesc = (session == null? "(not attached to a VaadinSession)" : getDescription(ui.getSession()));
                logger.log(Level.WARNING, "Found a Vaadin UI instance "+getDescription(object)+" outside your class name prefixes, suggest you add this: "+object.getClass().getName()+", Session: "+vsDesc);
            }

            if (object instanceof VaadinSession){
                logger.log(Level.WARNING, "Found a VaadinSession instance "+getDescription(object)+" outside your class name prefixes, suggest you add this: "+object.getClass().getName());
            }
        }
    }

    /**
     *
     * @param rootRef
     * @param fullyQualifiedClassnamePrefixes
     * @return
     */
    public static DeepSize calculateSizesOf(Object rootRef, String... fullyQualifiedClassnamePrefixes) {
        return new SizeOfCalculator(rootRef, fullyQualifiedClassnamePrefixes).calc();
    }

    private DeepSize calc() {
        final long deepSize = sizeOf.deepSizeOf(filteringVisitorListener, this.rootRef);

        ArrayList<ClassStatistics> classStatistics = new ArrayList<>();
        classnameToInstanceSizes.forEach((className, objectInstanceSizes) -> {
            ClassStatistics cts = new ClsSize(className, objectInstanceSizes.toArray(new InstanceStatistics[0]));
            classStatistics.add(cts);
        });

        Collections.sort(classStatistics);
        Collections.reverse(classStatistics);

        final ClassStatistics[] classStats = classStatistics.toArray(new ClassStatistics[0]);
        return new DeepSize() {
            @Override
            public long getDeepSize() {
                return deepSize;
            }

            @Override
            public ClassStatistics[] getClassStatistics() {
                return classStats;
            }
        };
    }
}
