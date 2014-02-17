package krasa.frameswitcher;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import com.intellij.openapi.diagnostic.Logger;
import javassist.ClassClassPath;
import javassist.ClassPool;
import krasa.frameswitcher.remote.RemoteCommunicator;
import krasa.frameswitcher.remote.domain.RemoteProject;
import krasa.frameswitcher.remote.domain.RemoteResult;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ExportableApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;

@State(name = "FrameSwitcherSettings", storages = { @Storage(id = "FrameSwitcherSettings", file = "$APP_CONFIG$/FrameSwitcherSettings.xml") })
public class FrameSwitcherApplicationComponent implements ApplicationComponent,
		PersistentStateComponent<FrameSwitcherSettings>, ExportableApplicationComponent, Configurable {
	private static final Logger LOG= Logger.getInstance("#" + FrameSwitcherApplicationComponent .class.getName());

	private FrameSwitcherSettings settings;
	private FrameSwitcherGui gui;

	Multimap<String, RemoteProject> remoteProjectMultimap = ArrayListMultimap.create();
	protected RemoteCommunicator remoteCommunicator;

	public Multimap<String, RemoteProject> getRemoteProjectMultimap() {
		return remoteProjectMultimap;
	}

	public static FrameSwitcherApplicationComponent getInstance() {
		return ServiceManager.getService(FrameSwitcherApplicationComponent.class);
	}

	public FrameSwitcherApplicationComponent() {
	}

	public RemoteCommunicator getRemoteCommunicator() {
		return remoteCommunicator;
	}

	public void initComponent() {
		ClassPool.getDefault().insertClassPath(new ClassClassPath(FrameSwitcherApplicationComponent.class));

		try {
			remoteCommunicator = new RemoteCommunicator();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		reloadRemoteProjects();
		//load active projects so they are not removed with the first ping
		remoteCommunicator.ping();
	}

	public void reloadRemoteProjects() {
		LOG.info("reloadRemoteProjects");
		remoteCommunicator.fetchRemoteProjects(new FCFutureResultHandler<RemoteResult>() {
			@Override
			public void resultReceived(RemoteResult remoteResult, String s) {
				LOG.info(Thread.currentThread().getName() + " received result, roundtrip time "
						+ (System.currentTimeMillis() - remoteResult.getTimeStamp()));
				remoteProjectMultimap.removeAll(remoteResult.getUuid());
				remoteProjectMultimap.putAll(remoteResult.getUuid(), remoteResult.getRemoteProjects());
			}
		});
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "FrameSwitcherApplicationComponent";
	}

	@NotNull
	@Override
	public File[] getExportFiles() {
		return new File[] { PathManager.getOptionsFile("FrameSwitcherSettings") };
	}

	@NotNull
	@Override
	public String getPresentableName() {
		return "FrameSwitcher";
	}

	@Nullable
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
	}

	@Override
	public void reset() {
		gui.importFrom(settings);
	}

	@Override
	public void disposeUIResources() {
		gui = null;
	}

	public void remoteProjectOpened(String uuid, RemoteProject remoteProject) {
		remoteProjectMultimap.put(uuid, remoteProject);
	}

	public void projectClosed(String uuid, RemoteProject remoteProject) {
		remoteProjectMultimap.remove(uuid, remoteProject);
	}


	public void sweepRemoteInstance() {
		// just hope that all instances returned result in time
		final List<String> activeInstances = remoteCommunicator.getActiveInstances();
		final Set<String> cacheRemoteInstances = remoteProjectMultimap.keySet();
		for (String uuid : activeInstances) {
			if (!cacheRemoteInstances.contains(uuid)) {
				reloadRemoteProjects();
				return;
			}
		}

		final String[] cacheRemoteInstancesArray = cacheRemoteInstances.toArray(new String[cacheRemoteInstances.size()]);
		for (String uuid : cacheRemoteInstancesArray) {
			if (!activeInstances.contains(uuid)) {
				LOG.info("remote instance removed uuid=" + uuid);
				remoteProjectMultimap.removeAll(uuid);
			}
		}
		activeInstances.clear();
	}
}
