package space.ruiwang;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.provider.RpcServer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
public class TomcatServer implements RpcServer {

    @Override
    public void start(String hostName, int port) {
        Tomcat tomcat = new Tomcat();

        Server server = tomcat.getServer();
        Service service = server.findService("Tomcat");

        Connector connector = new Connector();
        connector.setPort(port);

        Engine engine = new StandardEngine();
        engine.setDefaultHost(hostName);

        Host host = new StandardHost();
        host.setName(hostName);

        String contextPath = "";
        Context context = new StandardContext();
        context.setPath(contextPath);
        context.addLifecycleListener(new Tomcat.FixContextListener());

        host.addChild(context);
        engine.addChild(host);

        service.setContainer(engine);
        service.addConnector(connector);

        tomcat.addServlet(contextPath, "dispatcher", new DispatcherServlet());
        context.addServletMappingDecoded("/*", "dispatcher");

        try {
            tomcat.start();
            log.info("Provider[{}-{}] 启动成功", hostName, port);
            log.info("\n"
                    + " ___        ________   ________   ________                 _________   ________   _____ ______    ________   ________   _________   \n"
                    + "|\\  \\      |\\   __  \\ |\\   __  \\ |\\   ____\\               |\\___   ___\\|\\   __  \\ |\\   _ \\  _   \\ |\\   ____\\ |\\   __  \\ |\\___   ___\\ \n"
                    + "\\ \\  \\     \\ \\  \\|\\  \\\\ \\  \\|\\  \\\\ \\  \\___|   ____________\\|___ \\  \\_|\\ \\  \\|\\  \\\\ \\  \\\\\\__\\ \\  \\\\ \\  \\___| \\ \\  \\|\\  \\\\|___ \\  \\_| \n"
                    + " \\ \\  \\     \\ \\   _  _\\\\ \\   ____\\\\ \\  \\     |\\____________\\   \\ \\  \\  \\ \\  \\\\\\  \\\\ \\  \\\\|__| \\  \\\\ \\  \\     \\ \\   __  \\    \\ \\  \\  \n"
                    + "  \\ \\  \\____ \\ \\  \\\\  \\|\\ \\  \\___| \\ \\  \\____\\|____________|    \\ \\  \\  \\ \\  \\\\\\  \\\\ \\  \\    \\ \\  \\\\ \\  \\____ \\ \\  \\ \\  \\    \\ \\  \\ \n"
                    + "   \\ \\_______\\\\ \\__\\\\ _\\ \\ \\__\\     \\ \\_______\\                  \\ \\__\\  \\ \\_______\\\\ \\__\\    \\ \\__\\\\ \\_______\\\\ \\__\\ \\__\\    \\ \\__\\\n"
                    + "    \\|_______| \\|__|\\|__| \\|__|      \\|_______|                   \\|__|   \\|_______| \\|__|     \\|__| \\|_______| \\|__|\\|__|     \\|__|\n"
                    + "                                                                                                                                    \n"
                    + "                                                                                                                                    \n"
                    + "                                                                                                                                    \n");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            log.error("Server Tomcat启动失败: {}", e.getMessage());
        }
    }
}
