package krasa.frameswitcher.networking;

import com.intellij.openapi.project.Project;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.jgroups.Message;

import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public class DummyRemoteSender implements RemoteSender {
	@Override
	public void sendInstanceStarted() {

	}

	@Override
	public void sendProjectsState() {

	}

	@Override
	public void dispose() {

	}

	@Override
	public void asyncPing() {

	}

	@Override
	public void asyncProjectOpened(Project project) {

	}

	@Override
	public void sendProjectClosed(Project project) {

	}

	@Override
	public void openProject(UUID target, RemoteProject remoteProject) {

	}

	@Override
	public void sendPingResponse(Message msg) {

	}

	@Override
	public void asyncSendRefresh() {
		
	}
}
