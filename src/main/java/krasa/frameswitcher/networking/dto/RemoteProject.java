package krasa.frameswitcher.networking.dto;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

public class RemoteProject implements Serializable {

	private final String name;
	private final String projectPath;

	public RemoteProject(String name, String projectPath) {
		this.name = name;
		this.projectPath = projectPath;
	}

	public RemoteProject(Project project) {
		this(project.getName(), project.getBasePath());
	}

	public RemoteProject(ReopenProjectAction project) {
		this(project.getProjectName(), project.getProjectPath());
	}

	public String getName() {
		return name;
	}

	public String getProjectPath() {
		return projectPath;
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

		if (projectPath != null ? !projectPath.equals(that.projectPath) : that.projectPath != null) {
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
		result = 31 * result + (projectPath != null ? projectPath.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
