package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.core.ChainBaseManager;
import org.tron.core.store.DelegationStore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
@Slf4j(topic = "API")
public class GetDelegationServlet extends RateLimiterServlet {

  @Autowired
  private ChainBaseManager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long cycle = Long.parseLong(request.getParameter("cycle"));
      byte[] address = Commons.decodeFromBase58Check(request.getParameter("address"));
      String template = "reward: %d%nvote: %d%nvi: %s";
      DelegationStore delegationStore = manager.getDelegationStore();
      response.getWriter().println(String.format(template,
          delegationStore.getReward(cycle, address),
          delegationStore.getWitnessVote(cycle, address),
          delegationStore.getWitnessVi(cycle, address).divide(DelegationStore.DECIMAL_OF_VI_REWARD)));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
