package org.jboss.arquillian.container.openshift.ping;

import org.jboss.as.controller.ModelController;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class OpenShiftService implements Service<OpenShiftService> 
{
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("openshift", "ping");
    
    static ModelController controller;
    
    private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();

    public static void addService(final ServiceTarget serviceTarget) {
        OpenShiftService service = new OpenShiftService();
        
        serviceTarget.addService(OpenShiftService.SERVICE_NAME, service)
            .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedModelController)
            .install();
    }

    @Override
    public OpenShiftService getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        controller = injectedModelController.getValue();
    }

    @Override
    public void stop(StopContext context) {
        controller = null;
    }
}