package krasa.frameswitcher.remote.domain;

import com.intellij.openapi.project.Project;
import java.io.Serializable;

public class  RemoteProject implements Serializable {

	private final String name;
	private final String basePath;

	public RemoteProject(String name, String basePath) {
		this.name = name;
		this.basePath = basePath;
	}

	public RemoteProject(Project project) {
		this(project.getName(), project.getBasePath());
	}

	public String getName() {
		return name;
	}

	public String getBasePath() {
		return basePath;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		RemoteProject that = (RemoteProject) o;

		if (basePath != null ? !basePath.equals(that.basePath) : that.basePath != null) {
			return false;
		}
		if (name != null ? !name.equals(that.name) : that.name != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (basePath != null ? basePath.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RemoteProject{" +
				"name='" + name + '\'' +
				'}';
	}
}
