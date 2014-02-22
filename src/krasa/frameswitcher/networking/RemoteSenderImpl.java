package krasa.frameswitcher.networking;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import krasa.frameswitcher.FrameSwitchAction;
import krasa.frameswitcher.networking.dto.InstanceClosed;
import krasa.frameswitcher.networking.dto.InstanceStarted;
import krasa.frameswitcher.networking.dto.OpenProject;
import krasa.frameswitcher.networking.dto.Ping;
import krasa.frameswitcher.networking.dto.PingResponse;
import krasa.frameswitcher.networking.dto.ProjectClosed;
import krasa.frameswitcher.networking.dto.ProjectOpened;
import krasa.frameswitcher.networking.dto.ProjectsState;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.util.List;
import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public class RemoteSenderImpl implements RemoteSender {
	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	private UUID uuid;
	private JChannel channel;

	public RemoteSenderImpl(UUID uuid, final Receiver r) throws Exception {
		this.uuid = uuid;
		channel = new JChannel("frameswitcher-fast-local.xml");
//		channel = new JChannel("tcp.xml");
		channel.setReceiver(r);
		channel.connect("MyCluster");
		sendInstanceStarted();
	}

	@java.lang.Override
	public void sendInstanceStarted() {
		List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		final AnAction[] recentProjectsActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);

		LOG.info("sending InstanceStarted");
		send(new Message(null, new InstanceStarted(uuid, recentProjectsActions, ideFrames)));
	}

	@java.lang.Override
	public void sendProjectsState() {
		List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		final AnAction[] recentProjectsActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);
		LOG.info("sending ProjectsState");
		final Message msg = new Message(null, new ProjectsState(uuid, recentProjectsActions, ideFrames));
		send(msg);
	}

	@java.lang.Override
	public void close() {
		LOG.info("sending InstanceClosed");
		send(new Message(null, new InstanceClosed(uuid)));
		channel.close();
	}

	@java.lang.Override
	public void pingRemote() {
		LOG.info("sending Ping");
		send(new Message(null, new Ping(uuid)));
	}

	@java.lang.Override
	public void projectOpened(Project project) {
		LOG.info("sending ProjectOpened");
		send(new Message(null, new ProjectOpened(project.getName(), project.getBasePath(), uuid)));
	}

	@java.lang.Override
	public void sendProjectClosed(Project project) {
		LOG.info("sending ProjectClosed");
		send(new Message(null, new ProjectClosed(project.getName(), project.getBasePath(), uuid)));
	}

	@java.lang.Override
	public void openProject(UUID target, RemoteProject remoteProject) {
		final OpenProject openProject = new OpenProject(uuid, target, remoteProject);
		LOG.info("sending openProject");
		send(new Message(null, openProject));
	}

	@java.lang.Override
	public void sendPingResponse(Message msg) {
		LOG.info("sending PingResponse");
		send(new Message(msg.getSrc(), new PingResponse(uuid)));
	}

	private void send(Message msg) {
		try {
			channel.send(msg);
		} catch (Exception e) {
			LOG.warn(e);
		}
	}

	public JChannel getChannel() {
		return channel;
	}
}
