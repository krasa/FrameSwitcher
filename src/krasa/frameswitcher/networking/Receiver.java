package krasa.frameswitcher.networking;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import krasa.frameswitcher.FocusUtils;
import krasa.frameswitcher.FrameSwitchAction;
import krasa.frameswitcher.FrameSwitcherApplicationService;
import krasa.frameswitcher.networking.dto.*;
import org.jgroups.Message;

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

		ApplicationManager.getApplication().executeOnPooledThread( () -> {
			respond(msg, object);
		});
	}

	private void respond(Message msg, Object object) {
		if (object instanceof InstanceStarted) {
			instanceStarted((InstanceStarted) object);
		} else if (object instanceof ProjectsState) {
			updateRemoteProjectsState((ProjectsState) object);
		} else if (object instanceof Ping) {
			ping(msg);
		} else if (object instanceof PingResponse) {
			pingResponse((PingResponse) object);
		} else if (object instanceof ProjectOpened) {
			service.getRemoteInstancesState().projectOpened((ProjectOpened) object);
		} else if (object instanceof ProjectClosed) {
			service.getRemoteInstancesState().projectClosed((ProjectClosed) object);
		} else if (object instanceof InstanceClosed) {
			service.getRemoteInstancesState().instanceClosed((InstanceClosed) object);
		} else if (object instanceof OpenProject) {
			openProject((OpenProject) object);
		}
	}

	private void pingResponse(PingResponse object) {
		service.getRemoteInstancesState().processPingResponse(object);
	}

	private void ping(Message msg) {
		service.getRemoteSender().sendPingResponse(msg);
	}

	private void instanceStarted(InstanceStarted instanceStarted) {
		RemoteInstancesState remoteInstancesState = service.getRemoteInstancesState();
		remoteInstancesState.updateRemoteState(instanceStarted);
		service.getRemoteSender().sendProjectsState();
	}

	private void updateRemoteProjectsState(ProjectsState object) {
		RemoteInstancesState remoteInstancesState = service.getRemoteInstancesState();
		remoteInstancesState.updateRemoteState(object);
	}

	private void openProject(OpenProject openProject) {
		if (openProject.getTargetUUID().equals(uuid)) {
			List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
			for (final IdeFrame ideFrame : ideFrames) {
				final Project project = ideFrame.getProject();
				if (project != null && openProject.getProject().getProjectPath().equals(project.getBasePath())) {
					SwingUtilities.invokeLater(() -> {
						IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
							FocusUtils.requestFocus(project, false, true);
						});
					});
				}
			}
		}
	}

}
