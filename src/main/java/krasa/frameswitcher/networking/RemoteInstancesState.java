package krasa.frameswitcher.networking;

import com.intellij.openapi.diagnostic.Logger;
import krasa.frameswitcher.FrameSwitcherApplicationService;
import krasa.frameswitcher.networking.dto.*;

import java.util.*;

public class RemoteInstancesState {
	private static final Logger LOG = Logger.getInstance(RemoteInstancesState.class);

	public static final int TIMEOUT_millis = 10 * 1000;
	                                           
	volatile long  lastPingSend = 0;
	public Map<UUID, RemoteIdeInstance> remoteIdeInstances = new HashMap<>();

	public synchronized List<RemoteIdeInstance> getRemoteIdeInstances() {
		List<RemoteIdeInstance> list = new ArrayList<>(this.remoteIdeInstances.values());
		list.sort(Comparator.comparing(o -> o.ideName));
		return list;
	}

	public synchronized void sweepRemoteInstance() {
		for (Iterator<Map.Entry<UUID, RemoteIdeInstance>> iterator = remoteIdeInstances.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<UUID, RemoteIdeInstance> entry = iterator.next();
			RemoteIdeInstance value = entry.getValue();
			if (lastPingSend - value.lastResponse > TIMEOUT_millis) {
				LOG.debug("Removing " + value);
				iterator.remove();
			}
		}
	}

	public synchronized void processPingResponse(PingResponse object) {
		RemoteIdeInstance remoteIdeInstance = remoteIdeInstances.get(object.getUuid());
		if (remoteIdeInstance != null) {
			remoteIdeInstance.lastResponse = System.currentTimeMillis();
		} else {
			LOG.warn("DESYNC " + object);
			FrameSwitcherApplicationService.getInstance().getRemoteSender().asyncSendRefresh();
		}
	}

	public synchronized void updateRemoteState(ProjectsState instanceStarted) {
		RemoteIdeInstance remoteIdeInstance = getRemoteIdeInstance(instanceStarted);

		remoteIdeInstance.remoteRecentProjects.clear();
		remoteIdeInstance.remoteProjects.clear();

		remoteIdeInstance.remoteRecentProjects.addAll(instanceStarted.getRecentRemoteProjects());
		remoteIdeInstance.remoteProjects.addAll(instanceStarted.getRemoteProjects());
		remoteIdeInstance.ideName = instanceStarted.getName();
		remoteIdeInstance.lastResponse = System.currentTimeMillis();
	}

	public synchronized void projectClosed(ProjectClosed projectClosed) {
		RemoteIdeInstance remoteIdeInstance = getRemoteIdeInstance(projectClosed);
		remoteIdeInstance.remoteProjects.remove(projectClosed.getRemoteProject());
		remoteIdeInstance.remoteRecentProjects.add(projectClosed.getRemoteProject());
		remoteIdeInstance.lastResponse = System.currentTimeMillis();
	}

	public synchronized void projectOpened(ProjectOpened projectOpened) {
		RemoteIdeInstance remoteIdeInstance = getRemoteIdeInstance(projectOpened);
		remoteIdeInstance.remoteProjects.add(projectOpened.getRemoteProject());
		remoteIdeInstance.remoteRecentProjects.remove(projectOpened.getRemoteProject());
		remoteIdeInstance.lastResponse = System.currentTimeMillis();
	}

	private RemoteIdeInstance getRemoteIdeInstance(GeneralMessage message) {
		RemoteIdeInstance remoteIdeInstance = remoteIdeInstances.get(message.getUuid());
		if (remoteIdeInstance == null) {
			remoteIdeInstance = new RemoteIdeInstance(message.getUuid());
			remoteIdeInstances.put(remoteIdeInstance.uuid, remoteIdeInstance);
		}
		return remoteIdeInstance;
	}

	public synchronized void instanceClosed(InstanceClosed object) {
		remoteIdeInstances.remove(object.getUuid());
	}

	public synchronized void clear() {
		remoteIdeInstances.clear();
	}
}
