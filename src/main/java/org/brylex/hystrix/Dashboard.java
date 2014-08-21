package org.brylex.hystrix;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;

public class Dashboard {
    public static void main(String[] args) throws Exception {

        String path = new File("hystrix-dashboard.war").getAbsolutePath();
        System.err.println("Path: " + path);

        final Server server = new Server(9090);

        final HandlerCollection handlerCollection = new HandlerCollection();

        WebAppContext context = new WebAppContext();
        context.setContextPath("/dashboard/");
        context.setWar(path);
        handlerCollection.addHandler(context);

        server.setHandler(handlerCollection);
        server.start();
        server.join();
    }
}
