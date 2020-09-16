package krasa.frameswitcher;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ProjectWindowAction;
import com.intellij.testFramework.MapDataContext;
import com.intellij.util.BitUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author Vojtech Krasa
 */
public class FocusUtils {


	public static void requestFocus(Project project, final boolean useRobot, boolean custom) {
		JFrame frame = WindowManager.getInstance().getFrame(project);
		if (frame == null) {
			return;
		}
		if (custom) {
			requestFocusCustom(project, useRobot, frame);
		} else {
			final ProjectWindowAction windowAction = new ProjectWindowAction(project.getPresentableUrl(), project.getPresentableUrl(), null);
			final ProjectWindowAction next = windowAction.getNext();
			if (next != null) {
				KeyEvent a = new KeyEvent(frame, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
				AnActionEvent frameSwitcher = AnActionEvent.createFromInputEvent(a, "FrameSwitcher", null, new MapDataContext());
				next.setSelected(frameSwitcher, true);
			}
		}
	}

	public static void requestFocusCustom(Project project, boolean useRobot, JFrame frame) {
		boolean skip = false;

		if (!skip) {
			// the only reliable way I found to bring it to the top
			boolean aot = frame.isAlwaysOnTop();
			frame.setAlwaysOnTop(true);
			frame.setAlwaysOnTop(aot);
		}

		int frameState = frame.getExtendedState();
		if (BitUtil.isSet(frameState, Frame.ICONIFIED)) {
			// restore the frame if it is minimized
			frame.setExtendedState(BitUtil.set(frameState, Frame.ICONIFIED, false));
		}
		frame.toFront();

		IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
			if (project.isDisposed()) {
				return;
			}
			Component mostRecentFocusOwner = frame.getMostRecentFocusOwner();
			if (mostRecentFocusOwner != null) {
				IdeFocusManager.getGlobalInstance().requestFocus(mostRecentFocusOwner, true);
			}
		});

		if (useRobot && runningOnWindows7()) {
			try {
				// remember the last location of mouse
				final Point oldMouseLocation = MouseInfo.getPointerInfo().getLocation();

				// simulate a mouse click on title bar of window
				Robot robot = new Robot();
				robot.mouseMove(frame.getX(), frame.getY());
				robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

				// move mouse to old location
				robot.mouseMove((int) oldMouseLocation.getX(), (int) oldMouseLocation.getY());
			} catch (Exception ex) {
				// just ignore exception, or you can handle it as you want
			} finally {
				frame.setAlwaysOnTop(false);
			}
		}
	}

	public static boolean runningOnWindows7() {
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		return "Windows 7".equals(osName) && "6.1".equals(osVersion);
	}

}
