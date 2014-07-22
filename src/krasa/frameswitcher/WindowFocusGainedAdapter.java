package krasa.frameswitcher;

import com.intellij.openapi.project.Project;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Vojtech Krasa
 */
public class WindowFocusGainedAdapter extends WindowAdapter {

	protected final Project project;
	protected final ProjectFocusMonitor projectFocusMonitor;

	public WindowFocusGainedAdapter(Project project) {
		this.project = project;
		projectFocusMonitor = FrameSwitcherApplicationComponent.getInstance().getProjectFocusMonitor();
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		projectFocusMonitor.focusGained(project);
	}
}
