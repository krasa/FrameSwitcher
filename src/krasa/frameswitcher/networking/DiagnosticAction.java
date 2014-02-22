package krasa.frameswitcher.networking;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import krasa.frameswitcher.FrameSwitcherApplicationComponent;

/**
 * @author Vojtech Krasa
 */
public class DiagnosticAction extends AnAction {
	public void actionPerformed(AnActionEvent e) {
		final RemoteSender remoteSender1 = FrameSwitcherApplicationComponent.getInstance().getRemoteSender();
		if (remoteSender1 instanceof RemoteSenderImpl) {
			final RemoteSenderImpl remoteSender = (RemoteSenderImpl) remoteSender1;
			Notification myNotification = new Notification("krasa.frameswitcher",
					"FrameSwitcher", remoteSender.getChannel().getProperties().replace(";", "\n"),
					NotificationType.INFORMATION, null);
			Notifications.Bus.notify(myNotification, getEventProject(e));
		}
	}
}
