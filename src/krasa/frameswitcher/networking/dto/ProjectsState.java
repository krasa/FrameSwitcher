package krasa.frameswitcher.networking.dto;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectsState extends GeneralMessage {

	private List<RemoteProject> recentRemoteProjects;
	private List<RemoteProject> remoteProjects;
	private String name;

	public ProjectsState(UUID uuid, AnAction[] recentProjectsActions, List<IdeFrame> ideFrames, String name) {
		super(uuid);
		recentRemoteProjects = new ArrayList<RemoteProject>(recentProjectsActions.length);
		this.name = name;
		for (AnAction action : recentProjectsActions) {
			ReopenProjectAction reopenProjectAction = (ReopenProjectAction) action;
			recentRemoteProjects.add(new RemoteProject(reopenProjectAction));
		}
		remoteProjects = new ArrayList<RemoteProject>(ideFrames.size());
		for (IdeFrame ideFrame : ideFrames) {
			Project project = ideFrame.getProject();
			if (project != null) {
				remoteProjects.add(new RemoteProject(project));
			}
		}
	}

	public String getName() {
		return name;
	}

	public List<RemoteProject> getRemoteProjects() {
		return remoteProjects;
	}

	public List<RemoteProject> getRecentRemoteProjects() {
		return recentRemoteProjects;
	}
}
