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
import space.ruiwang.api.transport.RpcProvider;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
public class TomcatServer implements RpcProvider {

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
            log.info("Provider-Tomcat[{}-{}] 启动成功", hostName, port);
            log.info("\n"
                    + "  _        _____    _____     _____            _____    _____     ____   __      __  _____   _____    ______   _____  \n"
                    + " | |      |  __ \\  |  __ \\   / ____|          |  __ \\  |  __ \\   / __ \\  \\ \\    / / |_   _| |  __ \\  |  ____| |  __ \\ \n"
                    + " | |      | |__) | | |__) | | |       ______  | |__) | | |__) | | |  | |  \\ \\  / /    | |   | |  | | | |__    | |__) |\n"
                    + " | |      |  _  /  |  ___/  | |      |______| |  ___/  |  _  /  | |  | |   \\ \\/ /     | |   | |  | | |  __|   |  _  / \n"
                    + " | |____  | | \\ \\  | |      | |____           | |      | | \\ \\  | |__| |    \\  /     _| |_  | |__| | | |____  | | \\ \\ \n"
                    + " |______| |_|  \\_\\ |_|       \\_____|          |_|      |_|  \\_\\  \\____/      \\/     |_____| |_____/  |______| |_|  \\_\\ (Tomcat)\n"
                    + "                                                                                                                      \n");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            log.error("Provider-Tomcat[{}-{}] 启动失败: {}", hostName, port, e.getMessage());
        }
    }
}
