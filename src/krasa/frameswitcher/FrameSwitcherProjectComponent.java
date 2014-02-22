package krasa.frameswitcher;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class FrameSwitcherProjectComponent implements ProjectComponent {
	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	private Project project;
	private FrameSwitcherApplicationComponent instance;

	public FrameSwitcherProjectComponent(Project project) {
		this.project = project;
		instance = FrameSwitcherApplicationComponent.getInstance();
	}

	public void initComponent() {
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "FrameSwitcherProjectComponent";
	}

	public void projectOpened() {
		if (instance.getRemoteSender() != null) {
			instance.getRemoteSender().projectOpened(project);
		}
	}

	public void projectClosed() {
		if (instance.getRemoteSender() != null) {
			instance.getRemoteSender().sendProjectClosed(project);
		}

	}

}
