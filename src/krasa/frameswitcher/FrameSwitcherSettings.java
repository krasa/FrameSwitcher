package krasa.frameswitcher;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.registry.Registry;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FrameSwitcherSettings {
	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	private JBPopupFactory.ActionSelectionAid popupSelectionAid = JBPopupFactory.ActionSelectionAid.SPEEDSEARCH;
	private List<String> recentProjectPaths = new ArrayList<String>();
	private String maxRecentProjects = "";
	private boolean remoting;
	private boolean defaultSelectionCurrentProject=true;
	private String requestFocusMs = "100";
	private boolean selectImmediately = false;

	public JBPopupFactory.ActionSelectionAid getPopupSelectionAid() {
		return popupSelectionAid;
	}

	public boolean shouldShow(RemoteProject recentProjectsAction) {
		return shouldShow(recentProjectsAction.getProjectPath());
	}

	public boolean shouldShow(ReopenProjectAction action) {
		return shouldShow(action.getProjectPath());
	}

	private boolean shouldShow(String projectPath) {
		if (recentProjectPaths.size() == 0) {
			return true;
		}
		try {
			File file = new File(projectPath);
			String canonicalPath = file.getCanonicalPath();
			for (String recentProjectPath : recentProjectPaths) {
				if (canonicalPath.startsWith(new File(recentProjectPath).getCanonicalPath())) {
					return true;
				}
			}
		} catch (IOException e) {
			LOG.warn(e);
		}
		return false;
	}

	public List<String> getRecentProjectPaths() {
		return recentProjectPaths;
	}

	public static FrameSwitcherSettings getInstance() {
		return FrameSwitcherApplicationComponent.getInstance().getState();
	}

	public String getMaxRecentProjects() {
		try {
			if (!StringUtils.isBlank(maxRecentProjects)) {
				//noinspection ResultOfMethodCallIgnored
				Integer.parseInt(maxRecentProjects);
			}
		} catch (Exception e) {
			maxRecentProjects = "";
		}
		return maxRecentProjects;
	}

	public void setPopupSelectionAid(JBPopupFactory.ActionSelectionAid popupSelectionAid) {
		this.popupSelectionAid = popupSelectionAid;
	}

	public void setRecentProjectPaths(List<String> recentProjectPaths) {
		this.recentProjectPaths = recentProjectPaths;
	}

	public void setMaxRecentProjects(final String maxRecentProjects) {
		this.maxRecentProjects = maxRecentProjects;
		try {
			if (!StringUtils.isBlank(this.maxRecentProjects)) {
				//noinspection ResultOfMethodCallIgnored
				Integer.parseInt(this.maxRecentProjects);
			}
		} catch (Exception e) {
			this.maxRecentProjects = "";
		}
	}

	public boolean isRemoting() {
		return remoting;
	}

	public void setRemoting(final boolean remoting) {
		this.remoting = remoting;
	}

	public void applyMaxRecentProjectsToRegistry() {
		try {
			if (!StringUtils.isBlank(maxRecentProjects)) {
				LOG.info("Changing Registry " + FrameSwitcherApplicationComponent.IDE_MAX_RECENT_PROJECTS + " to " + maxRecentProjects);
				Registry.get(FrameSwitcherApplicationComponent.IDE_MAX_RECENT_PROJECTS).setValue(Integer.parseInt(maxRecentProjects));
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void applyOrResetMaxRecentProjectsToRegistry() {
		if (!StringUtils.isBlank(maxRecentProjects)) {
			applyMaxRecentProjectsToRegistry();
		} else {
			LOG.info("Changing Registry " + FrameSwitcherApplicationComponent.IDE_MAX_RECENT_PROJECTS + " to default");
			Registry.get(FrameSwitcherApplicationComponent.IDE_MAX_RECENT_PROJECTS).resetToDefault();
		}
	}

	public boolean isDefaultSelectionCurrentProject() {
		return defaultSelectionCurrentProject;
	}

	public void setDefaultSelectionCurrentProject(final boolean defaultSelectionCurrentProject) {
		this.defaultSelectionCurrentProject = defaultSelectionCurrentProject;
	}

	public String getRequestFocusMs() {
		return requestFocusMs;
	}

	public void setRequestFocusMs(final String requestFocusMs) {
		this.requestFocusMs = requestFocusMs;
	}

	public boolean isSelectImmediately() {
		return selectImmediately;
	}

	public void setSelectImmediately(boolean selectImmediately) {
		this.selectImmediately = selectImmediately;
	}
}
