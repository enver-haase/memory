package com.example.application.views.helloworld;

import com.example.application.profiling.SizeOfCalculator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.example.application.views.MainLayout;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import java.util.logging.Level;
import java.util.logging.Logger;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class HelloWorldView extends HorizontalLayout {
    private final TextField name;

    Logger logger = Logger.getLogger(HelloWorldView.class.getName());


    public HelloWorldView() {

        addClassName("hello-world-flow-view");
        name = new TextField("Your name");
        Button sayHello = new Button("Say hello");

        Button logMemDump = new Button("Log Memory Dump");
        logMemDump.setDisableOnClick(true);
        logMemDump.addClickListener(e -> {
            System.gc();
            SizeOfCalculator.ClassTotalSize[] totals = SizeOfCalculator.calculateDeepSizesOf(VaadinSession.getCurrent(), "com.example.application.view");
            logger.log(Level.INFO, "Memory footprint:");
            for (SizeOfCalculator.ClassTotalSize total : totals) {
                logger.log(Level.INFO, "Class "+total.getClassName()+" x"+total.getObjectInstanceSizes().length+" instances, total memory: "+total.getTotalSize() );
            }
            logMemDump.setEnabled(true);
        });

        Button closeSession = new Button("Close Session", e -> {
            Page page = UI.getCurrent().getPage();
            page.open("http://www.infraleap.com");
            VaadinSession session = VaadinSession.getCurrent();
            session.getSession().invalidate();
            session.close();
        });

        sayHello.addClickListener(e -> {
            Notification.show("Hallo"+(name.getValue() == null?"":" "+name.getValue()));


        });

        add(name, sayHello, logMemDump, closeSession);
        setVerticalComponentAlignment(Alignment.END, name, sayHello);
    }

}
