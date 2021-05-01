package krasa.frameswitcher.networking;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import krasa.frameswitcher.FrameSwitchAction;
import krasa.frameswitcher.FrameSwitcherUtils;
import krasa.frameswitcher.networking.dto.*;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.conf.ProtocolStackConfigurator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public class RemoteSenderImpl implements RemoteSender {
	private final Logger LOG = Logger.getInstance(RemoteSenderImpl.class);

	private UUID uuid;
	private JChannel channel;

	public RemoteSenderImpl(UUID uuid, final Receiver r, int port) throws Exception {
		this.uuid = uuid;
		ProtocolStackConfigurator stackConfigurator = ConfiguratorFactory.getStackConfigurator("frameswitcher-fast-local.xml");
		List<ProtocolConfiguration> protocolStack = stackConfigurator.getProtocolStack();
		for (ProtocolConfiguration protocolConfiguration : protocolStack) {
			if (protocolConfiguration.getProtocolName().equals("UDP")) {
				Map<String, String> properties = protocolConfiguration.getProperties();
				properties.put("mcast_port", String.valueOf(port));
				break;
			}
		}
		channel = new JChannel(stackConfigurator);
//		channel = new JChannel("tcp.xml");
		channel.setReceiver(r);
		channel.connect("FrameSwitcher");
		sendInstanceStarted();
	}

	@Override
	public void sendInstanceStarted() {
		List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		final AnAction[] recentProjectsActions = FrameSwitcherUtils.getRecentProjectsManagerBase().getRecentProjectsActions(false);
		LOG.info("sending InstanceStarted");
		send(new ObjectMessage(null, new InstanceStarted(uuid, recentProjectsActions, ideFrames, getName())));
	}

	@Override
	public void sendProjectsState() {
		List<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		final AnAction[] recentProjectsActions = FrameSwitcherUtils.getRecentProjectsManagerBase().getRecentProjectsActions(false);
		final Message msg = new ObjectMessage(null, new ProjectsState(uuid, recentProjectsActions, ideFrames,getName()));
		send(msg);
	}

	private String getName() {
		return ApplicationInfo.getInstance().getFullApplicationName();
	}

	@Override
	public void close() {
		LOG.info("sending InstanceClosed");
		send(new ObjectMessage(null, new InstanceClosed(uuid)));
		channel.close();
	}

	@Override
	public void pingRemote() {
		LOG.info("sending Ping");
		send(new ObjectMessage(null, new Ping(uuid)));
	}

	@Override
	public void projectOpened(Project project) {
		LOG.info("sending ProjectOpened");
		send(new ObjectMessage(null, new ProjectOpened(project.getName(), project.getBasePath(), uuid)));
	}

	@Override
	public void sendProjectClosed(Project project) {
		LOG.info("sending ProjectClosed");
		send(new ObjectMessage(null, new ProjectClosed(project.getName(), project.getBasePath(), uuid)));
	}

	@Override
	public void openProject(UUID target, RemoteProject remoteProject) {
		final OpenProject openProject = new OpenProject(uuid, target, remoteProject);
		LOG.info("sending openProject");
		send(new ObjectMessage(null, openProject));
	}

	@Override
	public void sendPingResponse(Message msg) {
		LOG.info("sending PingResponse");
		send(new ObjectMessage(msg.getSrc(), new PingResponse(uuid)));
	}

	private void send(Message msg) {
		try {
			channel.send(msg);
		} catch (Throwable e) {
			LOG.warn(e);
		}
	}

	public JChannel getChannel() {
		return channel;
	}
}
