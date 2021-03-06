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
package org.jboss.tools.intellij.openshift.actions.project;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jboss.tools.intellij.openshift.actions.OdoAction;
import org.jboss.tools.intellij.openshift.tree.LazyMutableTreeNode;
import org.jboss.tools.intellij.openshift.tree.application.ProjectNode;
import org.jboss.tools.intellij.openshift.utils.odo.Odo;
import org.jboss.tools.intellij.openshift.utils.UIHelper;

import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.jboss.tools.intellij.openshift.Constants.GROUP_DISPLAY_ID;

public class DeleteProjectAction extends OdoAction {
  public DeleteProjectAction() {
    super(ProjectNode.class);
  }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent, TreePath path, Object selected, Odo odo) {
    ProjectNode projectNode = (ProjectNode) selected;
    if (Messages.NO == Messages.showYesNoDialog("Delete Project '" + projectNode.toString() + "'.\nAre you sure?", "Delete Project",
        Messages.getQuestionIcon())) {
        return;
    }
    CompletableFuture.runAsync(() -> {
        try {
          Notification notif = new Notification(GROUP_DISPLAY_ID, "Delete project", "Deleting project " + selected.toString(), NotificationType.INFORMATION);
          Notifications.Bus.notify(notif);
          odo.deleteProject(selected.toString());
          notif.expire();
          Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Delete project", "Project " + selected + " has been successfully deleted", NotificationType.INFORMATION));
          ((LazyMutableTreeNode)projectNode.getParent()).remove(projectNode);
        } catch (IOException e) {
          UIHelper.executeInUI(() -> Messages.showErrorDialog("Error: " + e.getLocalizedMessage(), "Delete project"));
        }
      });
  }
}
