package krasa.frameswitcher;

import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckBoxList;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class ReopenProjectsForm {
	private JPanel root;
	private CheckBoxList<AnAction> list;

	public ReopenProjectsForm(Project project) {
		DefaultActionGroup group = new DefaultActionGroup();
		new FrameSwitchAction().fillReopen(group);
		for (AnAction child : group.getChildActionsOrStubs()) {
			if (child instanceof Separator) {
				continue;
			}
			String text;
			if (child instanceof ReopenProjectAction) {
				text = ((ReopenProjectAction) child).getProjectDisplayName();
			} else {
				text = child.getTemplatePresentation().getText();
			}

			list.addItem(child, text, false);
		}

		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	public JPanel getRoot() {
		return root;
	}

	public List<AnAction> getCheckProjects() {
		DefaultListModel model = (DefaultListModel) list.getModel();
		final ArrayList<AnAction> selected = new ArrayList<>();
		Object[] objects = model.toArray();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			final JCheckBox cb = (JCheckBox) object;
			if (cb.isSelected()) {
				//IJ 12 compatibility
				selected.add((AnAction) list.getItemAt(i));
			}

		}
		return selected;
	}
}
