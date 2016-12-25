package krasa.frameswitcher;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Vojtech Krasa
 */
public class WindowFocusGainedAdapter extends WindowAdapter {

	private final Project project;
	private final JFrame frame;
	private final ProjectFocusMonitor projectFocusMonitor;

	public WindowFocusGainedAdapter(@NotNull Project project, @NotNull JFrame frame, @NotNull ProjectFocusMonitor projectFocusMonitor) {
		this.project = project;
		this.frame = frame;
		this.projectFocusMonitor = projectFocusMonitor;
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		if (!project.isDisposed()) {
			projectFocusMonitor.focusGained(project);
		}
	}

	public
	@NotNull
	JFrame getFrame() {
		return frame;
	}
}
