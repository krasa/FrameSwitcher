package krasa.frameswitcher.networking;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import krasa.frameswitcher.FrameSwitcherApplicationService;
import krasa.frameswitcher.Utils;
import krasa.frameswitcher.networking.dto.*;
import org.jetbrains.annotations.NotNull;
import org.jgroups.ChannelListener;
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
	public static final String FRAME_SWITCHER = "FrameSwitcher";
	private final Logger LOG = Logger.getInstance(RemoteSenderImpl.class);

	private UUID uuid;
	private JChannel channel;
	private StandbyDetector standbyDetector;

	public RemoteSenderImpl(FrameSwitcherApplicationService service, UUID uuid, final Receiver r, int port) throws Exception {
		this.uuid = uuid;
		ProtocolStackConfigurator stackConfigurator = ConfiguratorFactory.getStackConfigurator("frameswitcher-fast-local.xml");
		List<ProtocolConfiguration> protocolStack = stackConfigurator.getProtocolStack();
		for (ProtocolConfiguration protocolConfiguration : protocolStack) {
			if (protocolConfiguration.getProtocolName().equals("UDP")) {
				Map<String, String> properties = protocolConfiguration.getProperties();
				properties.put("mcast_port", String.valueOf(port));
//				properties.put("bind_addr", "127.0.0.1");
				break;
			}
		}
		channel = new JChannel(stackConfigurator);
//		channel = new JChannel("tcp.xml");
		channel.addChannelListener(new ChannelListener() {
			@Override
			public void channelConnected(JChannel channel) {
				LOG.debug("channelConnected ", channel);
			}

			@Override
			public void channelDisconnected(JChannel channel) {
				LOG.debug("channelDisconnected ", channel);
			}

			@Override
			public void channelClosed(JChannel channel) {
				LOG.debug("channelClosed ", channel);
			}
		});
		channel.setDiscardOwnMessages(true);
		channel.setReceiver(r);
		channel.connect(FRAME_SWITCHER);
		sendInstanceStarted();
		standbyDetector = new StandbyDetector(30 * 1000) {
			@Override
			public void standbyDetected() {
				channel.disconnect();
				service.getRemoteInstancesState().clear();
			}
		};
	}

	@Override
	public void sendInstanceStarted() {
		LOG.debug("sending InstanceStarted");
		send(new ObjectMessage(null, new InstanceStarted(uuid, getRecentProjectsActions(), Utils.getIdeFrames(), getName())));
	}

	@Override
	public void sendProjectsState() {
		send(new ObjectMessage(null, new ProjectsState(uuid, getRecentProjectsActions(), Utils.getIdeFrames(), getName())));
	}
	            
	@NotNull
	private AnAction[] getRecentProjectsActions() {
		return RecentProjectsManagerBase.getInstanceEx().getRecentProjectsActions(false);
	}
	            
	private String getName() {
		ApplicationInfo instance = ApplicationInfo.getInstance();
		return instance.getFullApplicationName() + " - " + instance.getBuild();
	}

	@Override
	public void dispose() {
		LOG.debug("sending InstanceClosed");
		send(new ObjectMessage(null, new InstanceClosed(uuid)));
		channel.close();
		standbyDetector.stop();
	}

	@Override
	public void asyncPing() {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			LOG.debug("sending Ping");
			standbyDetector.check();
			FrameSwitcherApplicationService.getInstance().getRemoteInstancesState().lastPingSend = System.currentTimeMillis();
			send(new ObjectMessage(null, new Ping(uuid)));
		});
	}

	@Override
	public void asyncProjectOpened(Project project) {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			LOG.debug("sending ProjectOpened");
			send(new ObjectMessage(null, new ProjectOpened(project.getName(), project.getBasePath(), uuid)));
		});
	}

	@Override
	public void sendProjectClosed(Project project) {
		LOG.debug("sending ProjectClosed");
		send(new ObjectMessage(null, new ProjectClosed(project.getName(), project.getBasePath(), uuid)));
	}

	@Override
	public void openProject(UUID target, RemoteProject remoteProject) {
		final OpenProject openProject = new OpenProject(uuid, target, remoteProject);
		LOG.debug("sending openProject");
		send(new ObjectMessage(null, openProject));
	}

	@Override
	public void sendPingResponse(Message msg) {
		LOG.debug("sending PingResponse");
		send(new ObjectMessage(msg.getSrc(), new PingResponse(uuid)));
	}

	@Override
	public void asyncSendRefresh() {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			LOG.debug("sending Refresh");
			standbyDetector.check();
			sendInstanceStarted();
		});
	}

	private void send(Message msg) {
		try {
			if (!channel.isConnected()) {
				LOG.debug("reconnecting");
				channel.connect(FRAME_SWITCHER);
				sendInstanceStarted();
			}
			channel.send(msg);
		} catch (Throwable e) {
			LOG.warn(e);
		}
	}

	public JChannel getChannel() {
		return channel;
	}
}
