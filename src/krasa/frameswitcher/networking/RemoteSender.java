package krasa.frameswitcher.networking;

import com.intellij.openapi.project.Project;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.jgroups.Message;

import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public interface RemoteSender {
	void sendInstanceStarted();

	void sendProjectsState();

	void dispose();

	void asyncPing();

	void asyncProjectOpened(Project project);

	void sendProjectClosed(Project project);

	void openProject(UUID target, RemoteProject remoteProject);

	void sendPingResponse(Message msg);

	void asyncSendRefresh();
}
