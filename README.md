Arquillian Containers for Red Hat OpenShift
===========================================

Register on https://openshift.redhat.com/app/ to get free access!

OpenShift Express
-----------------

This container requires user to have create a domain and application using rhc commands:

* rhc-create-domain
* rhc-create-app

This will establish a remote Git repository and provide user with credentials which are required
in order to use the container.


Specify following configuration in arquillian.xml file:
* *namespace* - a namespace created by rhc-create-domain tool, e.g. bar
* *application* - an application name created by rhc-create-app tool, e.g. foo
* *login* - a Red Hat login (RHN with OpenShift Express access, e.g. bar@redhat.com
* *sshUserName* - an user name generated when an application is created by rhc-create-app tool, e.g.
* a7b1daad5c624157bdeea60b26cf8eba

Following configuration properties have sensible defaults, but can be modified:
* *type* - cartridge type, e.g. jbossas-7.0
* *libraDomain* - domain where OpenShift server instance is running, e.g. rhcloud.com

Note: For jbossas-7.0 cartridge automatic deployment must be disabled
