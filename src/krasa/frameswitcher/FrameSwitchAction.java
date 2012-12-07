package krasa.frameswitcher;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.*;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

	@Override
	protected void fillActions(final Project project, DefaultActionGroup group, DataContext dataContext) {
		ArrayList<IdeFrame> list = getIdeFrames();
		for (final IdeFrame frame : list) {
			final Project project1 = frame.getProject();
			if (project1 != null) {
				AnAction action = new AnAction(project1.getName()) {
					@Override
					public void actionPerformed(AnActionEvent e) {
						JComponent component = frame.getComponent();
						JFrame frame1 = WindowManager.getInstance().getFrame(project1);
						frame1.setVisible(true);
						frame1.setState(Frame.NORMAL);
						component.grabFocus();
					}
				};
				if (project != null) {
					VirtualFile projectFile = project.getProjectFile();
					VirtualFile projectFile1 = project1.getProjectFile();
					if (projectFile != null && projectFile1 != null) {
						boolean enabled = !projectFile.getPath().equals(projectFile1.getPath());
						action.getTemplatePresentation().setEnabled(enabled);
					}
				}
				group.addAction(action);
			}
		}
	}

	private ArrayList<IdeFrame> getIdeFrames() {
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
				return project1.getName().compareTo(project2.getName());
			}
		});
		return list;
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}
}
