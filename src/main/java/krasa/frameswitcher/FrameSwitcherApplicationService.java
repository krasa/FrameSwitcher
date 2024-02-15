package krasa.frameswitcher;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import krasa.frameswitcher.networking.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@State(name = "FrameSwitcherSettings", storages = {@Storage("FrameSwitcherSettings.xml")})
public class FrameSwitcherApplicationService implements PersistentStateComponent<FrameSwitcherSettings>, Disposable {

	private static final Logger LOG = Logger.getInstance(FrameSwitcherApplicationService.class);

	public static final String IDE_MAX_RECENT_PROJECTS = "ide.max.recent.projects";

	private FrameSwitcherSettings settings = new FrameSwitcherSettings();
	private RemoteInstancesState remoteInstancesState = new RemoteInstancesState();
	private ProjectFocusMonitor projectFocusMonitor = new ProjectFocusMonitor();
	private RemoteSender remoteSender = new DummyRemoteSender();
	boolean initialized;

	public static FrameSwitcherApplicationService getInstance() {
		FrameSwitcherApplicationService service = ApplicationManager.getApplication().getService(FrameSwitcherApplicationService.class);
		if (service != null && !service.initialized) { //DynamicPlugins.unloadPlugin causes null
			service.initComponent();
		}
		return service;
	}

	public FrameSwitcherApplicationService() {
	}

	public void initComponent() {
		long start = System.currentTimeMillis();
		initRemoting();
		LOG.debug("initComponent done in ", System.currentTimeMillis() - start, "ms");
	}

	private synchronized void initRemoting() {
		if (!initialized) {
			if (this.settings.isRemoting()) {
				if (!(remoteSender instanceof RemoteSenderImpl)) {
					try {
						UUID uuid = getState().getOrInitializeUuid();
						remoteSender = new RemoteSenderImpl(this, uuid, new Receiver(uuid, this), getState().getPort());
					} catch (Throwable e) {
						remoteInstancesState = new RemoteInstancesState();
						remoteSender = new DummyRemoteSender();
						LOG.error(e);
					}
				}
			} else {
				if (remoteSender instanceof RemoteSenderImpl) {
					remoteSender.dispose();
				}

				if (!(remoteSender instanceof DummyRemoteSender)) {
					remoteInstancesState = new RemoteInstancesState();
					remoteSender = new DummyRemoteSender();
				}
			}
			initialized = true;
		}
	}

	public ProjectFocusMonitor getProjectFocusMonitor() {
		return projectFocusMonitor;
	}

	@NotNull
	@Override
	public FrameSwitcherSettings getState() {
		return settings;
	}

	@Override
	public void loadState(FrameSwitcherSettings frameSwitcherSettings) {
		this.settings = frameSwitcherSettings;
		frameSwitcherSettings.applyMaxRecentProjectsToRegistry();
	}

	public void updateSettings(FrameSwitcherSettings settings) {
		this.settings = settings;
		this.settings.applyOrResetMaxRecentProjectsToRegistry();
		initRemoting();
	}

	public RemoteInstancesState getRemoteInstancesState() {
		return remoteInstancesState;
	}

	public RemoteSender getRemoteSender() {
		return remoteSender;
	}

	@Override
	public void dispose() {
		long start = System.currentTimeMillis();
		if (getRemoteSender() != null) {
			getRemoteSender().dispose();
		}
		LOG.debug("disposeComponent done in ", System.currentTimeMillis() - start, "ms");
	}
}
