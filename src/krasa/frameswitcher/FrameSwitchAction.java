package krasa.frameswitcher;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

    @Override
    protected void fillActions(Project project, DefaultActionGroup group, DataContext dataContext) {
        IdeFrame[] allProjectFrames = WindowManager.getInstance().getAllProjectFrames();
        for (final IdeFrame frame : allProjectFrames) {
            Project project1 = frame.getProject();
            if (project1 != null) {
                group.addAction(new AnAction(project1.getName()) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        frame.getComponent().grabFocus();
                    }
                });
            }
        }
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }
}
