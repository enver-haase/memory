package com.example.application.views.helloworld;

import com.example.application.profiling.SizeOfCalculator;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@UIScope
public class HelloWorldView extends HorizontalLayout {


    Logger logger = Logger.getLogger(HelloWorldView.class.getName());

    private final List<byte[]> memoryWaste = new ArrayList<>();

    public HelloWorldView() {
        addClassName("hello-world-flow-view");
        Button wasteMemory = new Button("Waste memory");

        Button logMemDump = new Button("Log Memory Dump");
        logMemDump.setDisableOnClick(true);
        logMemDump.addClickListener(e -> {
            System.gc();
            SizeOfCalculator.ClassTotalSize[] totals = SizeOfCalculator.calculateDeepSizesOf(VaadinSession.getCurrent(), "com.example.application.view");
            logger.log(Level.INFO, "Memory footprint:");
            for (SizeOfCalculator.ClassTotalSize total : totals) {
                logger.log(Level.INFO, "Class "+total.getClassName()+" x"+total.getObjectInstanceSizes().length+" instances, total memory: "+total.getTotalSize() );
                for (SizeOfCalculator.ObjectInstanceSize ois : total.getObjectInstanceSizes()){
                    logger.log(Level.INFO, "---Instance "+ois.getDescription()+", size "+ois.getDeepSize());
                }
            }
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

}
