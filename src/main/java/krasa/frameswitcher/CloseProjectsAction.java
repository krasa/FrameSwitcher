package krasa.frameswitcher;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;

import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class CloseProjectsAction extends DumbAwareAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		final CloseProjectsForm form = new CloseProjectsForm(getEventProject(anActionEvent));

		DialogBuilder builder = new DialogBuilder(getEventProject(anActionEvent));
		builder.setCenterPanel(form.getRoot());
		builder.setDimensionServiceKey("FrameSwitcherCloseProjects");
		builder.setTitle("Close Projects");
		builder.removeAllActions();
		builder.addOkAction();
		builder.addCancelAction();

		boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
		if (isOk) {
			ProjectManagerEx projectManagerEx = ProjectManagerEx.getInstanceEx();
			RecentProjectsManagerBase recentProjectsManagerBase = RecentProjectsManagerBase.getInstanceEx();
			List<Project> checkProjects = form.getCheckProjects();
			for (Project checkProject : checkProjects) {
				if (!checkProject.isDisposed()) {
					projectManagerEx.closeAndDispose(checkProject);
					recentProjectsManagerBase.updateLastProjectPath();
				}
			}
			WelcomeFrame.showIfNoProjectOpened();
		}
	}

}
