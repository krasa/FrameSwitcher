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
	private FrameSwitcherApplicationComponent applicationComponent;
	private WindowFocusGainedAdapter focusGainedAdapter;

	public FrameSwitcherProjectComponent(Project project) {
		this.project = project;
		applicationComponent = FrameSwitcherApplicationComponent.getInstance();
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

		focusGainedAdapter = new WindowFocusGainedAdapter(project, frame, applicationComponent.getProjectFocusMonitor());
		frame.addWindowFocusListener(focusGainedAdapter);
		focusGainedAdapter.windowGainedFocus(null);

		if (applicationComponent.getRemoteSender() != null) {
			applicationComponent.getRemoteSender().projectOpened(project);
		}
	}

	public void projectClosed() {
		if (applicationComponent.getRemoteSender() != null) {
			applicationComponent.getRemoteSender().sendProjectClosed(project);
		}

		ProjectFocusMonitor projectFocusMonitor = applicationComponent.getProjectFocusMonitor();
		projectFocusMonitor.projectClosed(project);


		if (focusGainedAdapter != null) {
			JFrame frame = focusGainedAdapter.getFrame();
			frame.removeWindowFocusListener(focusGainedAdapter);
		}
	}

}
