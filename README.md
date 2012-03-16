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

* _namespace_ - a namespace created by rhc-create-domain tool, e.g. bar
* _application_ - an application name created by rhc-create-app tool, e.g. foo
* _login_ - a Red Hat login (RHN with OpenShift Express access, e.g. bar@redhat.com
* _sshUserName_ - an user name generated when an application is created by rhc-create-app tool, e.g. a7b1daad5c624157bdeea60b26cf8eba

Following configuration properties have sensible defaults, but can be modified:

* _type_ - cartridge type, e.g. jbossas-7.0
* _libraDomain_ - domain where OpenShift server instance is running, e.g. rhcloud.com
* _deploymentTimeoutInSeconds_ - timeout in seconds to wait for a deployment to be finished

Following configuration properties are optional

* _passphrase_ - the passphrase to SSH identity key, can be set via SSH_PASSPHRASE environment variable
* _identityFile_ - the path to SSH identity key (must be absolute), can be set via SSH_IDENTITYFILE environment variable
* _disableStrictHostChecking_ - set it to true to disable StrictHostChecking policy 

For jbossas-7.0 cartridge automatic deployment is disabled during execution of the tests. This means your application
built from pom.xml is not available during testing. A workaround is to use a different application name 
and repository for testing.

Note: Requires Maven Surefire plugin 2.9 or higher, because of [SUREFIRE-743](http://jira.codehaus.org/browse/SUREFIRE-743)
