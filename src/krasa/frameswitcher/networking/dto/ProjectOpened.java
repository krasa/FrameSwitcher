package krasa.frameswitcher.networking.dto;

import java.util.UUID;

public class ProjectOpened extends GeneralMessage {
	private RemoteProject remoteProject;

	public ProjectOpened(String name, String basePath, UUID uuid) {
		super(uuid);
		remoteProject = new RemoteProject(name, basePath);
	}

	public RemoteProject getRemoteProject() {
		return remoteProject;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ProjectOpened that = (ProjectOpened) o;

		if (remoteProject != null ? !remoteProject.equals(that.remoteProject) : that.remoteProject != null) {
			return false;
		}
		if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = uuid != null ? uuid.hashCode() : 0;
		result = 31 * result + (remoteProject != null ? remoteProject.hashCode() : 0);
		return result;
	}

	public void setRemoteProject(RemoteProject remoteProject) {
		this.remoteProject = remoteProject;
	}
}
