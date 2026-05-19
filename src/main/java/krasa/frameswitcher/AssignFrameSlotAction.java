package krasa.frameswitcher;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AssignFrameSlotAction extends DumbAwareAction {

	public static final String ID_PREFIX = "krasa.frameswitcher.AssignFrameSlot";

	private final int slot;

	protected AssignFrameSlotAction(int slot) {
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
		Project project = e.getData(CommonDataKeys.PROJECT);
		if (project == null) {
			notify("Open a project first to assign it to frame slot " + slot + ".");
			return;
		}
		String path = project.getBasePath();
		if (path == null || path.isEmpty()) {
			notify("Current project has no base path; cannot assign to a frame slot.");
			return;
		}
		toggle(slot, path);
	}

	public static void toggle(int slot, String projectPath) {
		FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();
		Integer existing = settings.getSlotForPath(projectPath);
		if (existing != null && existing == slot) {
			settings.clearSlot(slot);
		} else {
			settings.assignSlot(slot, projectPath);
		}
	}

	static void notify(String message) {
		Notifications.Bus.notify(new Notification("Frame Switcher plugin",
				"Frame Switcher", message, NotificationType.INFORMATION));
	}

	public static class Slot1 extends AssignFrameSlotAction { public Slot1() { super(1); } }
	public static class Slot2 extends AssignFrameSlotAction { public Slot2() { super(2); } }
	public static class Slot3 extends AssignFrameSlotAction { public Slot3() { super(3); } }
	public static class Slot4 extends AssignFrameSlotAction { public Slot4() { super(4); } }
	public static class Slot5 extends AssignFrameSlotAction { public Slot5() { super(5); } }
	public static class Slot6 extends AssignFrameSlotAction { public Slot6() { super(6); } }
	public static class Slot7 extends AssignFrameSlotAction { public Slot7() { super(7); } }
	public static class Slot8 extends AssignFrameSlotAction { public Slot8() { super(8); } }
	public static class Slot9 extends AssignFrameSlotAction { public Slot9() { super(9); } }
}
