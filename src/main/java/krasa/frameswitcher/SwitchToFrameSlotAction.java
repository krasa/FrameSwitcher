package krasa.frameswitcher;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Objects;

public abstract class SwitchToFrameSlotAction extends DumbAwareAction {

	public static final String ID_PREFIX = "krasa.frameswitcher.SwitchToFrameSlot";

	private final int slot;

	protected SwitchToFrameSlotAction(int slot) {
		this.slot = slot;
	}

	public int getSlot() {
		return slot;
	}

	public static String idFor(int slot) {
		return ID_PREFIX + slot;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();
		String path = settings.getSlotPath(slot);
		if (path == null || path.isEmpty()) {
			AssignFrameSlotAction.notify("Frame slot " + slot + " is not assigned. Open the FrameSwitcher popup (Alt+F2) and press the Assign Frame Slot " + slot + " shortcut on a frame to assign it.");
			return;
		}

		Project opened = findOpenProject(path);
		if (opened != null) {
			switchTo(opened);
			return;
		}

		ReopenProjectAction reopen = new ReopenProjectAction(path, displayName(path), path);
		int prev = GeneralSettings.getInstance().getConfirmOpenNewProject();
		try {
			GeneralSettings.getInstance().setConfirmOpenNewProject(GeneralSettings.OPEN_PROJECT_NEW_WINDOW);
			reopen.actionPerformed(new AnActionEvent(null, e.getDataContext(),
					"FrameSwitcher-SlotReopen", reopen.getTemplatePresentation(),
					ActionManager.getInstance(), 0));
		} finally {
			SwingUtilities.invokeLater(() -> GeneralSettings.getInstance().setConfirmOpenNewProject(prev));
		}
	}

	private static Project findOpenProject(String path) {
		for (Project p : ProjectManager.getInstance().getOpenProjects()) {
			if (p.isDisposed()) continue;
			if (Objects.equals(p.getBasePath(), path) || Objects.equals(p.getProjectFilePath(), path)) {
				return p;
			}
		}
		return null;
	}

	private static void switchTo(Project project) {
		SwingUtilities.invokeLater(() -> IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
				() -> FocusUtils.requestFocus(project, false, false)));
	}

	private static String displayName(String path) {
		return new File(path).getName();
	}

	public static class Slot1 extends SwitchToFrameSlotAction { public Slot1() { super(1); } }
	public static class Slot2 extends SwitchToFrameSlotAction { public Slot2() { super(2); } }
	public static class Slot3 extends SwitchToFrameSlotAction { public Slot3() { super(3); } }
	public static class Slot4 extends SwitchToFrameSlotAction { public Slot4() { super(4); } }
	public static class Slot5 extends SwitchToFrameSlotAction { public Slot5() { super(5); } }
	public static class Slot6 extends SwitchToFrameSlotAction { public Slot6() { super(6); } }
	public static class Slot7 extends SwitchToFrameSlotAction { public Slot7() { super(7); } }
	public static class Slot8 extends SwitchToFrameSlotAction { public Slot8() { super(8); } }
	public static class Slot9 extends SwitchToFrameSlotAction { public Slot9() { super(9); } }
}
