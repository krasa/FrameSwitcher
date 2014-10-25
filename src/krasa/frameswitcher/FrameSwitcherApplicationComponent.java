package krasa.frameswitcher;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import krasa.frameswitcher.networking.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.UUID;

@State(name = "FrameSwitcherSettings", storages = {@Storage(id = "FrameSwitcherSettings", file = "$APP_CONFIG$/FrameSwitcherSettings.xml")})
public class FrameSwitcherApplicationComponent implements ApplicationComponent,
		PersistentStateComponent<FrameSwitcherSettings>, ExportableApplicationComponent, Configurable {
	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());


	private FrameSwitcherSettings settings;
	private FrameSwitcherGui gui;
	private RemoteInstancesState remoteInstancesState = new RemoteInstancesState();
	private RemoteSender remoteSender;
	private ProjectFocusMonitor projectFocusMonitor;

	public static FrameSwitcherApplicationComponent getInstance() {
		return ServiceManager.getService(FrameSwitcherApplicationComponent.class);
	}

	public FrameSwitcherApplicationComponent() {
	}

	public ProjectFocusMonitor getProjectFocusMonitor() {
		if (projectFocusMonitor == null) {
			projectFocusMonitor = new ProjectFocusMonitor();
		}
		return projectFocusMonitor;
	}

	public void initComponent() {
		if (getState().isRemoting()) {
			initRemoting();
		} else {
			remoteSender = new DummyRemoteSender();
		}
	}

	private void initRemoting() {
		UUID uuid = UUID.randomUUID();
		try {
			remoteSender = new RemoteSenderImpl(uuid, new Receiver(uuid, this));
		} catch (Throwable e) {
			LOG.warn(e);
		}
	}

	public void disposeComponent() {
		if (getRemoteSender() != null) {
			getRemoteSender().close();
		}
	}


	@NotNull
	public String getComponentName() {
		return "FrameSwitcherApplicationComponent";
	}

	@NotNull
	@Override
	public File[] getExportFiles() {
		return new File[]{PathManager.getOptionsFile("FrameSwitcherSettings")};
	}

	@NotNull
	@Override
	public String getPresentableName() {
		return "Frame Switcher";
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
	}

	// Configurable---------------------------------
	@Nls
	@Override
	public String getDisplayName() {
		return "FrameSwitcher";
	}

	@Nullable
	public Icon getIcon() {
		return null;
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		if (gui == null) {
			gui = new FrameSwitcherGui(getState());
		}
		return gui.getRoot();
	}

	@Override
	public boolean isModified() {
		return gui.isModified(getState());
	}

	@Override
	public void apply() throws ConfigurationException {
		settings = gui.exportDisplayedSettings();

		if (remoteSender instanceof DummyRemoteSender && settings.isRemoting()) {
			initRemoting();
		} else if (remoteSender instanceof RemoteSenderImpl && !settings.isRemoting()) {
			remoteSender.close();
			remoteInstancesState = new RemoteInstancesState();
			remoteSender = new DummyRemoteSender();
		}
	}

	@Override
	public void reset() {
		gui.importFrom(settings);
	}

	@Override
	public void disposeUIResources() {
		gui = null;
	}

	public RemoteInstancesState getRemoteInstancesState() {
		return remoteInstancesState;
	}

	public RemoteSender getRemoteSender() {
		return remoteSender;
	}

}
