package krasa.frameswitcher;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FrameSwitcherProjectComponent implements ProjectComponent {
	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	private Project project;
	private FrameSwitcherApplicationComponent instance;
	private WindowFocusGainedAdapter focusGainedAdapter;

	public FrameSwitcherProjectComponent(Project project) {
		this.project = project;
		focusGainedAdapter = new WindowFocusGainedAdapter(project);
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
		JFrame frame = WindowManager.getInstance().getFrame(project);
		frame.addWindowFocusListener(focusGainedAdapter);
		focusGainedAdapter.windowGainedFocus(null);
		if (instance.getRemoteSender() != null) {
			instance.getRemoteSender().projectOpened(project);
		}
	}

	public void projectClosed() {
		if (instance.getRemoteSender() != null) {
			instance.getRemoteSender().sendProjectClosed(project);
		}

		ProjectFocusMonitor projectFocusMonitor = FrameSwitcherApplicationComponent.getInstance().getProjectFocusMonitor();
		projectFocusMonitor.projectClosed(project);
		JFrame frame = WindowManager.getInstance().getFrame(project);
		if (frame != null) {
			frame.removeWindowFocusListener(focusGainedAdapter);
		}
	}

}
