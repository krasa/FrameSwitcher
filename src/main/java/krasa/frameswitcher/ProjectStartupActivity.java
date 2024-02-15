package krasa.frameswitcher;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.WindowManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProjectStartupActivity implements ProjectActivity, DumbAware {
	private static final Logger LOG = Logger.getInstance(ProjectStartupActivity.class);

	@Nullable
	@Override
	public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
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
				if (service == null) {
					return;
				}
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
		return null;
	}
}
