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

    private final HashMap<String, List<ObjectInstanceSize>> classnameToInstanceSizes = new HashMap<>();

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

    public interface ObjectInstanceSize extends Comparable<ObjectInstanceSize> {
        String getDescription();
        long getDeepSize();
    }

    public interface ClassStatistics extends Comparable<ClassStatistics> {
        String getClassName();
        long getCumulatedSize();
        ObjectInstanceSize[] getObjectInstanceSizes();
    }

    public interface DeepSize {
        long getDeepSize();
        ClassStatistics[] getClassStatisticss();
    }

    private static class ClsSize implements ClassStatistics {
        private final String className;
        private final ObjectInstanceSize[] objectInstanceSizes;

        ClsSize(String className, ObjectInstanceSize[] objectInstanceSizes) {
            this.className = className;
            this.objectInstanceSizes = objectInstanceSizes;
        }

        @Override
        public String getClassName() {
            return this.className;
        }

        @Override
        public long getCumulatedSize() {
            long retVal = 0;
            for (ObjectInstanceSize ois : objectInstanceSizes) {
                retVal += ois.getDeepSize();
            }
            return retVal;
        }

        @Override
        public ObjectInstanceSize[] getObjectInstanceSizes() {
            return objectInstanceSizes;
        }

        @Override
        public int compareTo(ClassStatistics o) {
            return Long.compare(this.getCumulatedSize(), o.getCumulatedSize());
        }
    }

    private static class ObjSize implements ObjectInstanceSize {
        private final String desc;
        private final long size;

        ObjSize(String description, long size) {
            this.desc = description;
            this.size = size;
        }

        @Override
        public String getDescription() {
            return desc;
        }

        @Override
        public long getDeepSize() {
            return size;
        }

        @Override
        public int compareTo(ObjectInstanceSize o) {
            return Long.compare(size, o.getDeepSize());
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
                    // We use shallow size for the time being.
                    ObjectInstanceSize ois = new ObjSize(getDescription(object), /*sizeOf.deepSizeOf(object)*/ size);
                    List<ObjectInstanceSize> others = SizeOfCalculator.this.classnameToInstanceSizes.computeIfAbsent(className, k -> new ArrayList<>());
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
            Collections.sort(objectInstanceSizes); // sort
            Collections.reverse(objectInstanceSizes); // sort

            ClassStatistics cts = new ClsSize(className, objectInstanceSizes.toArray(new ObjectInstanceSize[0]));
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
            public ClassStatistics[] getClassStatisticss() {
                return classStats;
            }
        };
    }
}
