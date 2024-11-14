package krasa.frameswitcher;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.PathUtil;

import java.util.*;

public class Utils {
	public static ArrayList<IdeFrame> getIdeFrames() {
		IdeFrame[] allProjectFrames = WindowManager.getInstance().getAllProjectFrames();
		ArrayList<IdeFrame> list = new ArrayList<IdeFrame>(allProjectFrames.length);
		list.addAll(Arrays.asList(allProjectFrames));
		Collections.sort(list, new Comparator<IdeFrame>() {

			@Override
			public int compare(IdeFrame o1, IdeFrame o2) {
				Project project1 = o1.getProject();
				Project project2 = o2.getProject();
				if (project1 == null && project2 == null) {
					return 0;
				}
				if (project1 == null) {
					return -1;
				}
				if (project2 == null) {
					return 1;
				}
				return project1.getName().compareToIgnoreCase(project2.getName());
			}
		});
		return list;
	}

	public static AnAction[] removeCurrentProjects(AnAction[] actions) {
		ProjectFocusMonitor projectFocusMonitor = FrameSwitcherApplicationService.getInstance()
				.getProjectFocusMonitor();
		Project[] projectsOrderedByFocus = projectFocusMonitor.getProjectsOrderedByFocus();

		if (projectsOrderedByFocus != null) {
			return Arrays.stream(actions).filter(action -> {
				return !(action instanceof ReopenProjectAction)
						|| !isOpen(projectsOrderedByFocus, (ReopenProjectAction) action);
			}).toArray(AnAction[]::new);
		}
		return actions;
	}

	private static boolean isOpen(Project[] openedProjects, ReopenProjectAction action) {
		String projectPath = PathUtil.toSystemDependentName(action.getProjectPath());
		for (Project openedProject : openedProjects) {
			if (openedProject == null) {
				continue;
			}
			if (StringUtil.equals(projectPath, PathUtil.toSystemDependentName(openedProject.getBasePath())) ||
					Objects.equals(PathUtil.toSystemDependentName(openedProject.getProjectFilePath()), projectPath)) {
				return true;
			}
		}
		return false;
	}
}
