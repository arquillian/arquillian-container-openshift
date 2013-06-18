package org.jboss.arquillian.container.openshift.ping;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

@WebServlet(name = "deployment", urlPatterns = { "/*" })
public class DeploymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String deploymentName = req.getParameter("name");
        if (deploymentName == null) {
            returnFalse(resp);
        }

        if (deploymentExists(deploymentName)) {
            returnTrue(resp);
        } else {
            returnFalse(resp);
        }
    }

    /**
     * @param deploymentName
     * @return
     */
    private boolean deploymentExists(String deploymentName) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(new ModelNode().add(DEPLOYMENT, deploymentName));

        ModelNode result = OpenShiftService.controller.execute(
                operation, 
                OperationMessageHandler.logging,
                ModelController.OperationTransactionControl.COMMIT, 
                null);

        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    private void returnTrue(HttpServletResponse resp) {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void returnFalse(HttpServletResponse resp) {
        resp.setContentType("text/plain");
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

}