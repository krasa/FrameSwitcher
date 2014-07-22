package krasa.frameswitcher;

import com.intellij.openapi.project.Project;

import java.util.LinkedHashSet;

/**
 * @author Vojtech Krasa
 */
public class ProjectFocusMonitor {

	private LinkedHashSet<Project> projects = new LinkedHashSet<Project>();

	public void focusGained(Project project) {
		projects.remove(project);
		projects.add(project);
	}

	public void projectClosed(Project project) {
		projects.remove(project);
	}

	public Project[] getProjectsOrderedByFocus() {
		return projects.toArray(new Project[projects.size()]);
	}

}
