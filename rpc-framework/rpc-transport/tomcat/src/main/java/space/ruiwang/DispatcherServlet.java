package space.ruiwang;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public class DispatcherServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        new HttpServletHandler().handle(req, resp);
    }
}
