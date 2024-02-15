package krasa.frameswitcher.networking;

import com.intellij.ide.DataManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import krasa.frameswitcher.FocusUtils;
import krasa.frameswitcher.FrameSwitchAction;
import krasa.frameswitcher.FrameSwitcherApplicationService;
import krasa.frameswitcher.networking.dto.*;
import org.jgroups.Message;
import org.jgroups.View;

import javax.swing.*;
import java.util.List;
import java.util.UUID;

public class Receiver implements org.jgroups.Receiver {

	private static final Logger LOG = Logger.getInstance(Receiver.class);

	private UUID uuid;
	private FrameSwitcherApplicationService service;

	public Receiver(UUID uuid, FrameSwitcherApplicationService service) {
		this.uuid = uuid;
		this.service = service;
	}

	@Override
	public void receive(Message msg) {
		Object object = msg.getObject();
		if (object instanceof GeneralMessage) {
			if (((GeneralMessage) object).getUuid().equals(uuid)) {
				return;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received: " + object);
		}

		asyncRespond(msg, object);
	}

	@Override
	public void viewAccepted(View new_view) {
		org.jgroups.Receiver.super.viewAccepted(new_view);
	}

	private void asyncRespond(Message msg, Object object) {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			if (object instanceof InstanceStarted) {
				service.getRemoteInstancesState().updateRemoteState((InstanceStarted) object);
				service.getRemoteSender().sendProjectsState();
			} else if (object instanceof ProjectsState) {
				service.getRemoteInstancesState().updateRemoteState((ProjectsState) object);
			} else if (object instanceof Ping) {
				service.getRemoteSender().sendPingResponse(msg);
			} else if (object instanceof PingResponse) {
				service.getRemoteInstancesState().processPingResponse((PingResponse) object);
			} else if (object instanceof ProjectOpened) {
				service.getRemoteInstancesState().projectOpened((ProjectOpened) object);
			} else if (object instanceof ProjectClosed) {
				service.getRemoteInstancesState().projectClosed((ProjectClosed) object);
			} else if (object instanceof InstanceClosed) {
				service.getRemoteInstancesState().instanceClosed((InstanceClosed) object);
			} else if (object instanceof OpenProject) {
				openProject((OpenProject) object);
			} else if (object instanceof Refresh) {
				service.getRemoteSender().sendProjectsState();
			}
		});
	}

	private void openProject(OpenProject openProject) {
		if (openProject.getTargetUUID().equals(uuid)) {
			RemoteProject openProjectProject = openProject.getProject();

			if (requestFocus(openProjectProject)) return;

			reopenProject(openProjectProject);
		}
	}

	private boolean requestFocus(RemoteProject openProjectProject) {
		List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		for (final IdeFrame ideFrame : ideFrames) {
			final Project project = ideFrame.getProject();

			if (project != null) {
				if (openProjectProject.getProjectPath().equals(project.getBasePath())) {
					SwingUtilities.invokeLater(() -> {
						IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
							FocusUtils.requestFocus(project, false, true);
						});
					});
					return true;
				}
			}
		}
		return false;
	}

	private void reopenProject(RemoteProject openProjectProject) {
		ApplicationManager.getApplication().invokeLater(() -> {
			DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> {
				ReopenProjectAction reopenProjectAction = new ReopenProjectAction(openProjectProject.getProjectPath(), openProjectProject.getName(), openProjectProject.getName());
				reopenProjectAction.actionPerformed(new AnActionEvent(null, dataContext, "FrameSwitcherRemote", new Presentation(), ActionManager.getInstance(), 0));

				SwingUtilities.invokeLater(() -> {
					IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
						requestFocus(openProjectProject);
					});
				});
			});
		}, ModalityState.nonModal());
	}

}
