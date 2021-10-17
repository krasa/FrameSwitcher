package krasa.frameswitcher;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProjectStartupActivity implements StartupActivity.Background, StartupActivity.DumbAware {
	private static final Logger LOG = Logger.getInstance(ProjectStartupActivity.class);

	@Override
	public void runActivity(@NotNull Project project) {
		FrameSwitcherApplicationService service = FrameSwitcherApplicationService.getInstance();

		long start = System.currentTimeMillis();
		JFrame frame = WindowManager.getInstance().getFrame(project);

		WindowFocusGainedAdapter focusGainedAdapter = new WindowFocusGainedAdapter(project, frame, service.getProjectFocusMonitor());
		frame.addWindowFocusListener(focusGainedAdapter);
		focusGainedAdapter.windowGainedFocus(null);

		if (service.getRemoteSender() != null) {
			service.getRemoteSender().asyncProjectOpened(project);
		}
		Disposer.register(project, new Disposable() {
			@Override
			public void dispose() {
				long start = System.currentTimeMillis();
				FrameSwitcherApplicationService service = FrameSwitcherApplicationService.getInstance();
				if (service.getRemoteSender() != null) {
					service.getRemoteSender().sendProjectClosed(project);
				}

				ProjectFocusMonitor projectFocusMonitor = service.getProjectFocusMonitor();
				projectFocusMonitor.projectClosed(project);


				JFrame frame = focusGainedAdapter.getFrame();
				frame.removeWindowFocusListener(focusGainedAdapter);
				LOG.debug("projectClosed done in ", System.currentTimeMillis() - start, "ms");
			}
		});
		LOG.debug("projectOpened done in ", System.currentTimeMillis() - start, "ms");
	}
}
