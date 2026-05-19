package krasa.frameswitcher;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ClearFrameSlotAction extends DumbAwareAction {

	public static final String ID = "krasa.frameswitcher.ClearFrameSlot";

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		if (project == null) {
			return;
		}
		String path = project.getBasePath();
		if (path == null || path.isEmpty()) {
			return;
		}
		FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();
		Integer existing = settings.getSlotForPath(path);
		if (existing == null) {
			AssignFrameSlotAction.notify("Current project is not assigned to any frame slot.");
			return;
		}
		settings.clearSlot(existing);
	}
}
