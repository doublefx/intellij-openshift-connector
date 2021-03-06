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
package org.jboss.tools.intellij.openshift.tree.application;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.jboss.tools.intellij.openshift.Constants;
import org.jboss.tools.intellij.openshift.tree.IconTreeNode;
import org.jboss.tools.intellij.openshift.tree.LazyMutableTreeNode;
import org.jboss.tools.intellij.openshift.utils.odo.Odo;
import org.jboss.tools.intellij.openshift.utils.odo.OdoCli;
import org.jboss.tools.intellij.openshift.utils.odo.OdoProjectDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.List;

import static com.intellij.openapi.ui.Messages.CANCEL_BUTTON;
import static com.intellij.openapi.ui.Messages.getWarningIcon;
import static org.jboss.tools.intellij.openshift.Constants.HELP_LABEL;
import static org.jboss.tools.intellij.openshift.Constants.CLUSTER_MIGRATION_ERROR_MESSAGE;
import static org.jboss.tools.intellij.openshift.Constants.CLUSTER_MIGRATION_MESSAGE;
import static org.jboss.tools.intellij.openshift.Constants.CLUSTER_MIGRATION_TITLE;
import static org.jboss.tools.intellij.openshift.Constants.UPDATE_LABEL;

public class ApplicationsRootNode extends LazyMutableTreeNode implements IconTreeNode {
  private OpenShiftClient client = loadClient();
  private boolean logged;
  private final ApplicationTreeModel model;
  private Odo odo;

  private static final String ERROR = "Please log in to the cluster";

  public ApplicationsRootNode(ApplicationTreeModel model) {
    setUserObject(client.getMasterUrl());
    this.model = model;
  }

  public OpenShiftClient getClient() {
    return client;
  }

  private OpenShiftClient loadClient() {
    return new DefaultOpenShiftClient(new ConfigBuilder().build());
  }

  public boolean isLogged() {
    return logged;
  }

  public void setLogged(boolean logged) {
    this.logged = logged;
  }

  public Odo getOdo() throws IOException {
    if (odo == null) {
        odo = new OdoProjectDecorator(OdoCli.get(), model);
    }
    return odo;
  }

  public ApplicationTreeModel getModel() {
    return model;
  }

  @Override
  public void load() {
    super.load();
    try {
      Odo odo = getOdo();
      odo.getProjects(client).stream().forEach(p -> add(new ProjectNode(p)));
      checkMigrate(odo, odo.getPreOdo10Projects(client));
      setLogged(true);
    } catch (Exception e) {
      add(new DefaultMutableTreeNode(ERROR));
    }
  }

  private void checkMigrate(Odo odo, List<Project> preOdo10Projects) {
    if (!preOdo10Projects.isEmpty()) {
      int choice = Messages.showDialog(getModel().getProject(), CLUSTER_MIGRATION_MESSAGE, CLUSTER_MIGRATION_TITLE, new String[]{UPDATE_LABEL, HELP_LABEL, CANCEL_BUTTON}, 0, getWarningIcon());
      if (choice == 0) {
        try {
          List<Exception> exceptions = ProgressManager.getInstance().run(
                  new Task.WithResult<List<Exception>, Exception>(getModel().getProject(), CLUSTER_MIGRATION_TITLE, false) {
                    private int counter = 0;

                    @Override
                    protected List<Exception> compute(@NotNull ProgressIndicator indicator) throws Exception {
                      return odo.migrateProjects(client, preOdo10Projects, (project, kind) -> {
                        indicator.setText("Migrating " + kind + " for project " + project);
                        indicator.setFraction(counter++ / (preOdo10Projects.size() * 8));
                      });
                    }
                  }

          );
          if (!exceptions.isEmpty()) {
            Messages.showErrorDialog(getModel().getProject(), CLUSTER_MIGRATION_ERROR_MESSAGE, CLUSTER_MIGRATION_TITLE);
          }
        } catch (Exception e) {
        }
      } else if (choice == 1) {
        BrowserUtil.browse(Constants.MIGRATION_HELP_PAGE_URL);
      }
    }
  }

  @Override
  public void reload() {
    client = loadClient();
    super.reload();
  }

  @Override
  public String getIconName() {
    return "/images/cluster.png";
  }
}
