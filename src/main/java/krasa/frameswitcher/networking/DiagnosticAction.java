package krasa.frameswitcher.networking;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import krasa.frameswitcher.FrameSwitcherApplicationService;

/**
 * @author Vojtech Krasa
 */
public class DiagnosticAction extends AnAction {
	@Override
	public void actionPerformed(AnActionEvent e) {
		final RemoteSender remoteSender1 = FrameSwitcherApplicationService.getInstance().getRemoteSender();
		if (remoteSender1 instanceof RemoteSenderImpl) {
			final RemoteSenderImpl remoteSender = (RemoteSenderImpl) remoteSender1;
			String content = remoteSender.getChannel().getProperties().replace(";", "\n");
			
			NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Frame Switcher plugin");
			Notification myNotification = group.createNotification("FrameSwitcher", content, NotificationType.INFORMATION);
			Notifications.Bus.notify(myNotification, getEventProject(e));
		}
	}
}
