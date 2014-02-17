package krasa.frameswitcher.remote.domain;

import java.io.Serializable;

public class OpenProject implements Serializable {
	private final String uuid;
	private final RemoteProject basePath;

	public String getUuid() {
		return uuid;
	}

	public RemoteProject getProject() {
		return basePath;
	}

	public OpenProject(String uuid, RemoteProject basePath) {
		this.uuid = uuid;
		this.basePath = basePath;
	}
}
