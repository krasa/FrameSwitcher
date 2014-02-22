package krasa.frameswitcher.networking;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.diagnostic.Logger;
import krasa.frameswitcher.networking.dto.InstanceClosed;
import krasa.frameswitcher.networking.dto.PingResponse;
import krasa.frameswitcher.networking.dto.ProjectClosed;
import krasa.frameswitcher.networking.dto.ProjectOpened;
import krasa.frameswitcher.networking.dto.ProjectsState;
import krasa.frameswitcher.networking.dto.RemoteProject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RemoteInstancesState {

	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());
	public static final int MAX_KEEP_ALIVE_TIME = 10000;

	public volatile Map<UUID, Date> forRemoval = new HashMap<UUID, Date>();
	public volatile List<UUID> actualActiveInstances = new ArrayList<UUID>();
	public volatile Multimap<UUID, RemoteProject> remoteProjects = ArrayListMultimap.<UUID, RemoteProject>create();
	public volatile Multimap<UUID, RemoteProject> remoteRecentProjects = ArrayListMultimap.<UUID, RemoteProject>create();

	public synchronized Multimap<UUID, RemoteProject> getRemoteRecentProjects() {
		return ArrayListMultimap.create(remoteRecentProjects);
	}

	public synchronized Multimap<UUID, RemoteProject> getRemoteProjects() {
		return ArrayListMultimap.create(remoteProjects);
	}

	public synchronized void sweepRemoteInstance() {
		// just hope that all instances returned result in time
		final Set<UUID> remoteProjectsKeys = remoteProjects.keySet();
		for (UUID uuid : actualActiveInstances) {
			if (!remoteProjectsKeys.contains(uuid)) {
				// //TODO
				LOG.warn("DESYNC");
				return;
			}
		}

		final UUID[] remoteInstances = remoteProjectsKeys.toArray(new UUID[remoteProjectsKeys.size()]);
		for (UUID remoteInstance : remoteInstances) {
			if (!actualActiveInstances.contains(remoteInstance)) {
				if (forRemoval.containsKey(remoteInstance)) {
					if (new Date().getTime() - forRemoval.get(remoteInstance).getTime() > MAX_KEEP_ALIVE_TIME) {
						remoteProjects.removeAll(remoteInstance);
						remoteRecentProjects.removeAll(remoteInstance);
						forRemoval.remove(remoteInstance);
					}
				} else {
					forRemoval.put(remoteInstance, new Date());
				}
			}
		}
		actualActiveInstances.clear();
	}

	public synchronized void processPingResponse(PingResponse object) {
		actualActiveInstances.add(object.getUuid());
		forRemoval.remove(object.getUuid());
	}

	public synchronized void updateRemoteState(ProjectsState instanceStarted) {
		remoteRecentProjects.removeAll(instanceStarted.getUuid());
		remoteProjects.removeAll(instanceStarted.getUuid());

		remoteRecentProjects.putAll(instanceStarted.getUuid(), instanceStarted.getRecentRemoteProjects());
		remoteProjects.putAll(instanceStarted.getUuid(), instanceStarted.getRemoteProjects());
	}

	public synchronized void projectClosed(ProjectClosed projectClosed) {
		remoteProjects.remove(projectClosed.getUuid(), projectClosed.getRemoteProject());
		remoteRecentProjects.put(projectClosed.getUuid(), projectClosed.getRemoteProject());
	}

	public synchronized void projectOpened(ProjectOpened projectOpened) {
		remoteProjects.put(projectOpened.getUuid(), projectOpened.getRemoteProject());
		remoteRecentProjects.remove(projectOpened.getUuid(), projectOpened.getRemoteProject());
	}

	public synchronized void instanceClosed(InstanceClosed object) {
		remoteProjects.removeAll(object.getUuid());
		remoteRecentProjects.removeAll(object.getUuid());
	}
}
