package krasa.frameswitcher;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import krasa.frameswitcher.networking.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@State(name = "FrameSwitcherSettings", storages = {@Storage( "FrameSwitcherSettings.xml")})
public class FrameSwitcherApplicationService implements  PersistentStateComponent<FrameSwitcherSettings>, Disposable {
	
	private static final Logger LOG = Logger.getInstance(FrameSwitcherApplicationService.class);
	
	public static final String IDE_MAX_RECENT_PROJECTS = "ide.max.recent.projects";


	private FrameSwitcherSettings settings;
	private RemoteInstancesState remoteInstancesState = new RemoteInstancesState();
	private RemoteSender remoteSender;
	private ProjectFocusMonitor projectFocusMonitor;
	boolean initialized;
	public static FrameSwitcherApplicationService getInstance() {
		FrameSwitcherApplicationService service = ServiceManager.getService(FrameSwitcherApplicationService.class);
		if (!service.initialized) {
			service.initComponent();
		}
		return service;
	}

	public FrameSwitcherApplicationService() {
	}

	public void initComponent() {
		long start = System.currentTimeMillis();
		initialized = true;
		if (getState().isRemoting()) {
			initRemoting();
		} else {
			remoteSender = new DummyRemoteSender();
		}
		LOG.debug("initComponent done in ", System.currentTimeMillis() - start, "ms");
	}

	public ProjectFocusMonitor getProjectFocusMonitor() {
		if (projectFocusMonitor == null) {
			projectFocusMonitor = new ProjectFocusMonitor();
		}
		return projectFocusMonitor;
	}

	private void initRemoting() {
		UUID uuid = getState().getOrInitializeUuid();
		try {
			remoteSender = new RemoteSenderImpl(uuid, new Receiver(uuid,this),getState().getPort());
		} catch (Throwable e) {
			remoteSender = new DummyRemoteSender();
			LOG.error(e);
		}
	}


	@NotNull
	@Override
	public FrameSwitcherSettings getState() {
		if (settings == null) {
			settings = new FrameSwitcherSettings();
		}
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
		if (remoteSender instanceof DummyRemoteSender && this.settings.isRemoting()) {
			initRemoting();
		} else if (remoteSender instanceof RemoteSenderImpl && !this.settings.isRemoting()) {
			remoteSender.close();
			remoteInstancesState = new RemoteInstancesState();
			remoteSender = new DummyRemoteSender();
		}
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
			getRemoteSender().close();
		}
		LOG.debug("disposeComponent done in ", System.currentTimeMillis() - start, "ms");
	}
}
