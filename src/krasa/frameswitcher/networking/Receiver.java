package krasa.frameswitcher.networking;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import krasa.frameswitcher.FocusUtils;
import krasa.frameswitcher.FrameSwitchAction;
import krasa.frameswitcher.FrameSwitcherApplicationComponent;
import krasa.frameswitcher.networking.dto.GeneralMessage;
import krasa.frameswitcher.networking.dto.InstanceClosed;
import krasa.frameswitcher.networking.dto.InstanceStarted;
import krasa.frameswitcher.networking.dto.OpenProject;
import krasa.frameswitcher.networking.dto.Ping;
import krasa.frameswitcher.networking.dto.PingResponse;
import krasa.frameswitcher.networking.dto.ProjectClosed;
import krasa.frameswitcher.networking.dto.ProjectOpened;
import krasa.frameswitcher.networking.dto.ProjectsState;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import java.util.List;
import java.util.UUID;

public class Receiver extends ReceiverAdapter {

	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	private UUID uuid;
	private FrameSwitcherApplicationComponent applicationComponent;

	public Receiver(UUID uuid, FrameSwitcherApplicationComponent applicationComponent) {
		this.uuid = uuid;
		this.applicationComponent = applicationComponent;
	}

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
		if (object instanceof InstanceStarted) {
			instanceStarted((InstanceStarted) object);
		} else if (object instanceof ProjectsState) {
			updateRemoteProjectsState((ProjectsState) object);
		} else if (object instanceof Ping) {
			ping(msg);
		} else if (object instanceof PingResponse) {
			pingResponse((PingResponse) object);
		} else if (object instanceof ProjectOpened) {
			applicationComponent.getRemoteInstancesState().projectOpened((ProjectOpened) object);
		} else if (object instanceof ProjectClosed) {
			applicationComponent.getRemoteInstancesState().projectClosed((ProjectClosed) object);
		} else if (object instanceof InstanceClosed) {
			applicationComponent.getRemoteInstancesState().instanceClosed((InstanceClosed) object);
		} else if (object instanceof OpenProject) {
			openProject((OpenProject) object);
		}

	}

	private void pingResponse(PingResponse object) {
		applicationComponent.getRemoteInstancesState().processPingResponse(object);
	}

	private void ping(Message msg) {
		applicationComponent.getRemoteSender().sendPingResponse(msg);
	}

	private void instanceStarted(InstanceStarted instanceStarted) {
		applicationComponent.getRemoteInstancesState().updateRemoteState(instanceStarted);
		applicationComponent.getRemoteSender().sendProjectsState();
	}

	private void updateRemoteProjectsState(ProjectsState object) {
		applicationComponent.getRemoteInstancesState().updateRemoteState(object);
	}

	private void openProject(OpenProject openProject) {
		if (openProject.getTargetUUID().equals(uuid)) {
			List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
			for (final IdeFrame ideFrame : ideFrames) {
				final Project project = ideFrame.getProject();
				if (project != null && openProject.getProject().getProjectPath().equals(project.getBasePath())) {
					ApplicationManager.getApplication().invokeLater(new Runnable() {

						@Override
						public void run() {
							FocusUtils.requestFocus(project, true);
						}
					});
				}
			}
		}
	}

}
