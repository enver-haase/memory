package com.example.application.views.helloworld;

import com.example.application.profiling.SizeOfCalculator;
import com.example.application.profiling.VaadinVisitor;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.example.application.profiling.ObjDescription.getDescription;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@UIScope
public class HelloWorldView extends VerticalLayout {


    //Logger logger = Logger.getLogger(HelloWorldView.class.getName());

    private final List<byte[]> memoryWaste = new ArrayList<>();

    //private final Grid test = new Grid(); // orphan!

    public HelloWorldView() {
        addClassName("hello-world-flow-view");
        Button wasteMemory = new Button("Waste memory");

        Button logMemDump = new Button("Print Memory Dump");
        logMemDump.setDisableOnClick(true);
        logMemDump.addClickListener(e -> {
            System.gc();

            Pre pre = new Pre();
            HelloWorldView.this.add(new Scroller(pre));

            String memProfile = getDebugMessage();

            pre.setText(memProfile);
            logMemDump.setEnabled(true);
        });

        Button closeSession = new Button("Close Session", e -> {
            VaadinSession session = VaadinSession.getCurrent();
            session.getSession().invalidate();
            session.close();
        });

        wasteMemory.addClickListener(e -> {
            memoryWaste.add(new byte[64*1024*1024]);
            Notification.show("I just wasted 64MB session size.");
        });

        add(wasteMemory, logMemDump, closeSession);
    }

    private String getDebugMessage() {
        StringBuilder retVal = new StringBuilder();

        VaadinService service = VaadinService.getCurrent();
        SizeOfCalculator.DeepSize deepSize = SizeOfCalculator.calculateSizesOf(service, service.getClass().getName(), "com.example.application.view", "com.vaadin.flow.spring.SpringVaadinSession", "com.vaadin.flow.component.internal.JavaScriptBootstrapUI");
        retVal.append("Memory footprint (deep size of ").append(getDescription(service)).append(" is ").append(deepSize.getDeepSize()).append(":\n");

        VaadinVisitor.VaadinStatistics vaadinStatistics = deepSize.getVaadinStatistics();
        int numCurrentComponents = vaadinStatistics.getComponentsOfCurrentUI().size();
        retVal.append("Current UI has ").append(numCurrentComponents).append(" Vaadin components.\n");
        int numCurrentUIs = vaadinStatistics.getUIsOfCurrentSession().size();
        retVal.append("Current VaadinSession has ").append(numCurrentUIs).append(" UIs.\n");
        int numOrphanedComponents = vaadinStatistics.getOrphanedComponents().size();
        if (numOrphanedComponents == 0){
            retVal.append("No orphaned components (not attached to any UI) found.\n");
        }
        else{
            retVal.append("WARNING: ").append(numOrphanedComponents).append(" ORPHANED COMPONENT(S) (not attached to any UI) FOUND!\n");
        }

        Set<String> sessions = vaadinStatistics.getSession2UIsMap().keySet();
        for (String session : sessions){
            retVal.append(session).append(":\n");
            Set<String> uis = vaadinStatistics.getSession2UIsMap().get(session);
            for (String ui : uis){
                retVal.append("  ").append(ui).append(": ").append(vaadinStatistics.getUI2ComponentsMap().get(ui).size()).append(" components.\n");
            }
        }
        retVal.append("\n");

        SizeOfCalculator.ClassStatistics[] totals = deepSize.getClassStatistics();
        for (SizeOfCalculator.ClassStatistics total : totals) {
            retVal.append("Class ").append(total.getClassName()).append(" x").append(total.getInstanceStatistics().length).append(" instances.\n");
            //for (SizeOfCalculator.InstanceStatistics ois : total.getInstanceStatistics()){
            //    retVal.append("---Instance ").append(ois.getDescription()).append("\n");
            //}
        }

        return retVal.toString();
    }

}
