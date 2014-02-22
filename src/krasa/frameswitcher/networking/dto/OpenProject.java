package krasa.frameswitcher.networking.dto;

import java.util.UUID;

public class OpenProject extends GeneralMessage {
	private final RemoteProject basePath;
	protected UUID targetUUID;

	public RemoteProject getProject() {
		return basePath;
	}

	public OpenProject(UUID uuid, UUID targetUUID, RemoteProject basePath) {
		super(uuid);
		this.targetUUID = targetUUID;
		this.basePath = basePath;
	}

	public UUID getTargetUUID() {
		return targetUUID;
	}
}
