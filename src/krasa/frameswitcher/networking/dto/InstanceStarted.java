package krasa.frameswitcher.networking.dto;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.wm.IdeFrame;

import java.util.List;
import java.util.UUID;

public class InstanceStarted extends ProjectsState {
	public InstanceStarted(UUID uuid, AnAction[] recentProjectsActions, List<IdeFrame> ideFrames) {
		super(uuid, recentProjectsActions, ideFrames);
	}
}
