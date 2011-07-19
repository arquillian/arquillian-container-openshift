package org.jboss.arquillian.container.openshift.express.ping;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceRegistryException;

public class OpenShiftActivator implements ServiceActivator {

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException 
    {
        OpenShiftService.addService(serviceActivatorContext.getServiceTarget());
    }
}
