package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.store.DynamicPropertiesStore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
@Slf4j(topic = "API")
public class GetDynamicPropertyServlet extends RateLimiterServlet {

  @Autowired
  private ChainBaseManager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      DynamicPropertiesStore dynamicPropertiesStore = manager.getDynamicPropertiesStore();
      Class<? extends DynamicPropertiesStore> clazz = dynamicPropertiesStore.getClass();
      Method method = clazz.getMethod(request.getParameter("method"));
      response.getWriter().println(method.invoke(dynamicPropertiesStore));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
