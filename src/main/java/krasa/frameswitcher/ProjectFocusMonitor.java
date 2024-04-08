package krasa.frameswitcher;

import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

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
		List<Project> sortedProjects = new ArrayList<>(projects);
		Collections.reverse(sortedProjects); // Reverse to get the latest focused project first
		return sortedProjects.toArray(new Project[0]);
	}

}
