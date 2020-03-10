package org.tron.core.services.http;

import java.io.IOException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetAccountLastUnwithdrawRewardServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] address = Util.getAddress(request);
      if (address != null) {
        HashMap<String, Long> value = wallet
            .computeUnwithdrawReward(address);
        response.getWriter().println(Util.printRewardMapToJSON(value));
      } else {
        response.getWriter().println("{}");
      }

    } catch (Exception e) {
      logger.error("", e);
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
