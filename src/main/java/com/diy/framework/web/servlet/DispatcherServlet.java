package com.diy.framework.web.servlet;

import com.diy.framework.context.ApplicationContext;
import com.diy.framework.web.method.HandlerMethod;
import com.diy.framework.web.method.RequestMappingInfo;
import com.diy.framework.web.mvc.Controller;
import com.diy.framework.web.mvc.anotation.RequestMethod;
import com.diy.framework.web.mvc.view.JspViewResolver;
import com.diy.framework.web.mvc.view.ModelAndView;
import com.diy.framework.web.mvc.view.UrlBasedViewResolver;
import com.diy.framework.web.mvc.view.View;
import com.diy.framework.web.mvc.view.ViewResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {

    private final List<ViewResolver> viewResolvers = new ArrayList<>();

    public DispatcherServlet() {
        this.viewResolvers.add(new UrlBasedViewResolver());
        this.viewResolvers.add(new JspViewResolver());
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String uri = req.getRequestURI();
        final String method = req.getMethod().toUpperCase();

        final RequestMethod requestMethod = RequestMethod.valueOf(method);

        final Object handler = getHandler(uri, requestMethod);

        if (handler == null) {
            return;
        }

        try {
            if (handler instanceof HandlerMethod handlerMethod) {
                final ModelAndView mav = handlerMethod.handle(req, resp);
                render(mav, req, resp);

                return;
            }

            if (handler instanceof Controller controller) {
                final ModelAndView mav = controller.handleRequest(req, resp);
                render(mav, req, resp);

                return;
            }

            throw new RuntimeException("not support request");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private Object getHandler(final String uri, final RequestMethod requestMethod) {
        final Map<RequestMappingInfo, Object> handlerMapping = ApplicationContext.handlerMapping;

        for (RequestMappingInfo mapping : handlerMapping.keySet()) {
            if (mapping.isMatch(uri, requestMethod)) {
                return handlerMapping.get(mapping);
            }
        }

        return null;
    }

    private void render(final ModelAndView mav, final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final String viewName = mav.getViewName();

        final View view = resolveViewName(viewName);

        if (view == null) {
            throw new RuntimeException("View not found: " + viewName);
        }

        view.render(mav.getModel(), req, resp);
    }

    private View resolveViewName(final String viewName) {
        for (final ViewResolver viewResolver : this.viewResolvers) {
            final View view = viewResolver.resolveViewName(viewName);
            if (view != null) {
                return view;
            }
        }

        return null;
    }

    private Map<String, ?> parseParams(final HttpServletRequest req) throws IOException {
        if ("application/json".equals(req.getHeader("Content-Type"))) {
            final byte[] bodyBytes = req.getInputStream().readAllBytes();
            final String body = new String(bodyBytes, StandardCharsets.UTF_8);

            return new ObjectMapper().readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } else {
            return req.getParameterMap();
        }
    }
}
