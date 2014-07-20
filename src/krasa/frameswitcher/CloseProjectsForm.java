package krasa.frameswitcher;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.CheckBoxList;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class CloseProjectsForm {
	private JPanel root;
	private CheckBoxList<Project> list;

	public CloseProjectsForm(Project project) {
		Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
		for (Project openProject : openProjects) {
			list.addItem(openProject, openProject.getName(), openProject != project);
		}
	}

	public JPanel getRoot() {
		return root;
	}

	public List<Project> getCheckProjects() {
		DefaultListModel model = (DefaultListModel) list.getModel();
		final ArrayList<Project> selected = new ArrayList<Project>();
		Object[] objects = model.toArray();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			final JCheckBox cb = (JCheckBox) object;
			if (cb.isSelected()) {
				selected.add(list.getItemAt(i));
			}

		}
		return selected;
	}
}
