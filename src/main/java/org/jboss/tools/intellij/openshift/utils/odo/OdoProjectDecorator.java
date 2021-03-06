/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.openshift.utils.odo;

import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import me.snowdrop.servicecatalog.api.model.ServiceInstance;
import org.jboss.tools.intellij.openshift.tree.application.ApplicationTreeModel;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class OdoProjectDecorator implements Odo {
    private final Odo delegate;
    private final ApplicationTreeModel model;

    private LocalConfig.ComponentSettings findComponent(String project, String application, String component) {
        Optional<ApplicationTreeModel.ComponentDescriptor> comp = model.getComponents().values().stream().filter(c -> c.getProject().equals(project) && c.getApplication().equals(application) && c.getName().equals(component)).findFirst();
        try {
            if (comp.isPresent()) {
                return comp.get().getSettings();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public OdoProjectDecorator(Odo delegate, ApplicationTreeModel model) {
        this.delegate = delegate;
        this.model = model;
    }

    @Override
    public List<io.fabric8.openshift.api.model.Project> getProjects(OpenShiftClient client) {
        return delegate.getProjects(client);
    }

    @Override
    public void describeApplication(String project, String application) throws IOException {
        delegate.describeApplication(project, application);
    }

    @Override
    public void deleteApplication(OpenShiftClient client, String project, String application) throws IOException {
        final IOException[] exception = {null};
        getComponents(client, project, application).forEach(component -> {
            try {
                deleteComponent(project, application, component.getPath(), component.getName(), component.getState() != ComponentState.NOT_PUSHED);
            } catch (IOException e) {
                exception[0] = e;
            }
        });
        if (exception[0] != null) {
            throw exception[0];
        }
    }

    @Override
    public void push(String project, String application, String context, String component) throws IOException {
        delegate.push(project, application, context, component);
    }

    @Override
    public void describeComponent(String project, String application, String context, String component) throws IOException {
        delegate.describeComponent(project, application, context, component);
    }

    @Override
    public void watch(String project, String application, String context, String component) throws IOException {
        delegate.watch(project, application, context, component);
    }

    @Override
    public void createComponentLocal(String project, String application, String componentType, String componentVersion, String component, String source, boolean push) throws IOException {
        delegate.createComponentLocal(project, application, componentType, componentVersion, component, source, push);

    }

    @Override
    public void createComponentGit(String project, String application, String context, String componentType, String componentVersion, String component, String source, String reference, boolean push) throws IOException {
        delegate.createComponentGit(project, application, context, componentType, componentVersion, component, source, reference, push);
    }

    @Override
    public void createComponentBinary(String project, String application, String context, String componentType, String componentVersion, String component, String source, boolean push) throws IOException {
        delegate.createComponentBinary(project, application, context, componentType, componentVersion, component, source, push);
    }

    @Override
    public void createService(String project, String application, String serviceTemplate, String servicePlan, String service) throws IOException {
        delegate.createService(project, application, serviceTemplate, servicePlan, service);
    }

    @Override
    public String getServiceTemplate(OpenShiftClient client, String project, String application, String service) {
        return delegate.getServiceTemplate(client, project, application, service);
    }

    @Override
    public void deleteService(String project, String application, String service) throws IOException {
        delegate.deleteService(project, application, service);
    }

    @Override
    public List<ComponentType> getComponentTypes() throws IOException {
        return delegate.getComponentTypes();
    }

    @Override
    public List<ServiceTemplate> getServiceTemplates() throws IOException {
        return delegate.getServiceTemplates();
    }

    @Override
    public void describeServiceTemplate(String template) throws IOException {
        delegate.describeServiceTemplate(template);
    }

    @Override
    public List<Integer> getServicePorts(OpenShiftClient client, String project, String application, String component) {
        return delegate.getServicePorts(client, project, application, component);
    }

    @Override
    public List<URL> listURLs(String project, String application, String context, String component) throws IOException {
        List<URL> urls = delegate.listURLs(project, application, context, component);
        LocalConfig.ComponentSettings settings = findComponent(project, application, component);
        if (settings != null) {
            settings.getUrls().forEach(url -> {
                if (urls.stream().noneMatch(url1 -> url1.getName().equals(url.getName()))) {
                    urls.add(URL.of(url.getName(), null, null, url.getPort(), ""));
                }
            });
        }
        return urls;
    }

    @Override
    public ComponentInfo getComponentInfo(OpenShiftClient client, String project, String application, String component) throws IOException {
        return delegate.getComponentInfo(client, project, application, component);
    }

    @Override
    public void createURL(String project, String application, String context, String component, String name, Integer port) throws IOException {
        delegate.createURL(project, application, context, component, name, port);
    }

    @Override
    public void deleteURL(String project, String application, String context, String component, String name) throws IOException {
        delegate.deleteURL(project, application, context, component, name);
    }

    @Override
    public void undeployComponent(String project, String application, String context, String component) throws IOException {
        delegate.undeployComponent(project, application, context, component);
    }

    @Override
    public void deleteComponent(String project, String application, String context, String component, boolean undeploy) throws IOException {
        delegate.deleteComponent(project, application, context, component, undeploy);
    }

    @Override
    public void follow(String project, String application, String context, String component) throws IOException {
        delegate.follow(project, application, context, component);
    }

    @Override
    public void log(String project, String application, String context, String component) throws IOException {
        delegate.log(project, application, context, component);
    }

    @Override
    public void createProject(String project) throws IOException {
        delegate.createProject(project);
    }

    @Override
    public void deleteProject(String project) throws IOException {
        delegate.deleteProject(project);
    }

    @Override
    public void login(String url, String userName, char[] password, String token) throws IOException {
        delegate.login(url, userName, password, token);
    }

    @Override
    public void logout() throws IOException {
        delegate.logout();
    }

    @Override
    public List<Application> getApplications(String project) throws IOException {
        List<Application> applications = delegate.getApplications(project);
        model.getComponents().forEach((path, component) -> {
           if (component.getProject().equals(project) && applications.stream().noneMatch(application -> application.getName().equals(component.getApplication()))) {
               applications.add(Application.of(component.getApplication()));
           }
        });
        return applications;
    }

    @Override
    public List<Component> getComponents(OpenShiftClient client, String project, String application) {
        List<Component> components = delegate.getComponents(client, project, application);
        model.getComponents().forEach((path, comp) -> {
            if (comp.getProject().equals(project) && comp.getApplication().equals(application)) {
                Optional<Component> found = components.stream().filter(comp1 -> comp1.getName().equals(comp.getName())).findFirst();
                if (found.isPresent()) {
                    found.get().setState(ComponentState.PUSHED);
                    found.get().setPath(path);
                } else {
                    components.add(Component.of(comp.getName(), ComponentState.NOT_PUSHED, path));
                }
            }
        });
        return components;
    }

    @Override
    public List<ServiceInstance> getServices(OpenShiftClient client, String project, String application) {
        return delegate.getServices(client, project, application);
    }

    @Override
    public List<Storage> getStorages(OpenShiftClient client, String project, String application, String component) {
        List<Storage> storages = delegate.getStorages(client, project, application, component);
        LocalConfig.ComponentSettings settings = findComponent(project, application, component);
        if (settings != null) {
            settings.getStorages().forEach(storage -> {
                if (storages.stream().noneMatch(storage1 -> storage1.getName().equals(storage.getName()))) {
                    storages.add(Storage.of(storage.getName()));
                }
            });
        }
        return storages;
    }

    @Override
    public void listComponents() throws IOException {
        delegate.listComponents();
    }

    @Override
    public void listServices() throws IOException {
        delegate.listServices();
    }

    @Override
    public void about() throws IOException {
        delegate.about();
    }

    @Override
    public void createStorage(String project, String application, String context, String component, String name, String mountPath, String storageSize) throws IOException {
        delegate.createStorage(project, application, context, component, name, mountPath, storageSize);
    }

    @Override
    public void deleteStorage(String project, String application, String context, String component, String storage) throws IOException {
        delegate.deleteStorage(project, application, context, component, storage);
    }

    @Override
    public void link(String project, String application, String component, String context, String source, Integer port) throws IOException {
        delegate.link(project, application, component, context, source, port);
    }

    @Override
    public List<Project> getPreOdo10Projects(OpenShiftClient client) {
        return delegate.getPreOdo10Projects(client);
    }

    @Override
    public List<Exception> migrateProjects(OpenShiftClient client, List<Project> projects, BiConsumer<String, String> reporter) {
        return delegate.migrateProjects(client, projects, reporter);
    }

    @Override
    public String consoleURL(OpenShiftClient client) throws IOException {
        return delegate.consoleURL(client);
    }
}
