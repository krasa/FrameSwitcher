package krasa.frameswitcher;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;

public class FrameSwitcherProjectComponent implements ProjectComponent {

	private Project project;

	public FrameSwitcherProjectComponent(Project project) {
		this.project = project;
	}

	public void initComponent() {

		// TODO: insert component initialization logic here
	}

	public void disposeComponent() {

		// TODO: insert component disposal logic here
	}

	@NotNull
	public String getComponentName() {
		return "FrameSwitcherProjectComponent";
	}

	public void projectOpened() {
		FrameSwitcherApplicationComponent instance = FrameSwitcherApplicationComponent.getInstance();
		instance.remoteCommunicator.projectOpened(project);
	}

	public void projectClosed() {
		FrameSwitcherApplicationComponent instance = FrameSwitcherApplicationComponent.getInstance();
		instance.remoteCommunicator.projectClosed(project);
	}
}
