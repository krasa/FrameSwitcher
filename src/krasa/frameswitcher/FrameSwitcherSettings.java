package krasa.frameswitcher;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FrameSwitcherSettings {

	private JBPopupFactory.ActionSelectionAid popupSelectionAid = JBPopupFactory.ActionSelectionAid.SPEEDSEARCH;
	private List<String> recentProjectPaths = new ArrayList<String>();
	private String maxRecentProjects = "25";

	public JBPopupFactory.ActionSelectionAid getPopupSelectionAid() {
		return popupSelectionAid;
	}

	public boolean shouldShow(ReopenProjectAction action) {
		if (recentProjectPaths.size() == 0) {
			return true;
		}
		try {
			File file = new File(action.getProjectPath());
			String canonicalPath = file.getCanonicalPath();
			for (String recentProjectPath : recentProjectPaths) {
				if (canonicalPath.startsWith(new File(recentProjectPath).getCanonicalPath())) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<String> getRecentProjectPaths() {
		return recentProjectPaths;
	}

	public static FrameSwitcherSettings getInstance() {
		FrameSwitcherApplicationComponent service = ServiceManager.getService(FrameSwitcherApplicationComponent.class);
		return service.getState();
	}

	public String getMaxRecentProjects() {
		return maxRecentProjects;
	}

	public int getMaxRecentProjectsAsInt() {
		return Integer.parseInt(maxRecentProjects);
	}

	public void setPopupSelectionAid(JBPopupFactory.ActionSelectionAid popupSelectionAid) {
		this.popupSelectionAid = popupSelectionAid;
	}

	public void setRecentProjectPaths(List<String> recentProjectPaths) {
		this.recentProjectPaths = recentProjectPaths;
	}

	public void setMaxRecentProjects(final String maxRecentProjects) {
		this.maxRecentProjects = maxRecentProjects;
	}
}
