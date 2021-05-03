package krasa.frameswitcher.networking;

import krasa.frameswitcher.networking.dto.RemoteProject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RemoteIdeInstance {
	public Set<RemoteProject> remoteRecentProjects = new HashSet<>();
	public Set<RemoteProject> remoteProjects = new HashSet<>();
	public String ideName = "";
	public long lastResponse = System.currentTimeMillis();
	public UUID uuid;

	public RemoteIdeInstance(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RemoteIdeInstance that = (RemoteIdeInstance) o;

		return uuid != null ? uuid.equals(that.uuid) : that.uuid == null;
	}

	@Override
	public int hashCode() {
		return uuid != null ? uuid.hashCode() : 0;
	}
}
