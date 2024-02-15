package krasa.frameswitcher;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;

import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class ReopenProjectsAction extends DumbAwareAction {
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		final ReopenProjectsForm form = new ReopenProjectsForm(getEventProject(anActionEvent));

		DialogBuilder builder = new DialogBuilder(getEventProject(anActionEvent));
		builder.setCenterPanel(form.getRoot());
		builder.setDimensionServiceKey("FrameSwitcherReopenProjects");
		builder.setTitle("Reopen Projects");
		builder.removeAllActions();
		builder.addOkAction();
		builder.addCancelAction();

		boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
		if (isOk) {
			List<AnAction> checkProjects = form.getCheckProjects();
			for (AnAction checkProject : checkProjects) {
				Runnable runnable = () -> {
					AnActionEvent frameSwitcherPlugin = new AnActionEvent(anActionEvent.getInputEvent(), DataContext.EMPTY_CONTEXT, "FrameSwitcherPlugin", getTemplatePresentation(), ActionManager.getInstance(), anActionEvent.getModifiers());
					checkProject.actionPerformed(frameSwitcherPlugin);
				};
				ApplicationManager.getApplication().invokeLater(runnable, ModalityState.nonModal());
			}
		}
	}

}
