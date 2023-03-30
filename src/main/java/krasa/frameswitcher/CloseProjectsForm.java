package krasa.frameswitcher;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.util.Function;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class CloseProjectsForm {
	private JPanel root;
	private CheckBoxList<Project> list;

	public CloseProjectsForm(Project project) {
		Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//IJ 12 compatibility
		list.setItems(Arrays.asList(openProjects), new Function<Project, String>() {
			@Override
			public String fun(Project project) {
				return project.getName();
			}
		});
		for (int i = 0; i < openProjects.length; i++) {
			Project openProject = openProjects[i];
			if (openProject != project) {
				list.setItemSelected(openProject, true);
			}
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
				//IJ 12 compatibility
				selected.add((Project) list.getItemAt(i));
			}

		}
		return selected;
	}
}
