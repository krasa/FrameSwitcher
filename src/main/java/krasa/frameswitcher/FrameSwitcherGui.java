package krasa.frameswitcher;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameSwitcherGui {

	private JPanel root;
	private JTextField maxRecentProjects;
	private JComboBox popupAidComboBox;

	private JList recentProjectFiltersList;
	private DefaultListModel filterListModel;
	private JButton addButton;
	private JButton remove;
	private JCheckBox remoting;

	private JCheckBox defaultSelectionCurrentProject;
	private JTextField requestFocusMs;
	private JCheckBox loadProjectIcon;

	private JButton addInclude;
	private JButton removeInclude;
	private JList includeProjectList;
	private DefaultListModel includeListModel;

	private FrameSwitcherSettings settings;
	private EnumComboBoxModel<JBPopupFactory.ActionSelectionAid> comboBoxModel;

	public FrameSwitcherGui(FrameSwitcherSettings settings) {
		this.settings = settings;

		addButton.addActionListener(e -> browseForFile(FrameSwitcherGui.this.filterListModel));
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int leadSelectionIndex = recentProjectFiltersList.getSelectionModel().getLeadSelectionIndex();
				if (!recentProjectFiltersList.getSelectionModel().isSelectionEmpty()) {
					filterListModel.remove(leadSelectionIndex);
				}
			}
		});
		addInclude.addActionListener(e -> browseForFile(FrameSwitcherGui.this.includeListModel));
		removeInclude.addActionListener(e -> {
			int leadSelectionIndex = includeProjectList.getSelectionModel().getLeadSelectionIndex();
			if (!includeProjectList.getSelectionModel().isSelectionEmpty()) {
				includeListModel.remove(leadSelectionIndex);
			}
		});
		initModel(settings);
	}

	private void initModel(FrameSwitcherSettings settings) {
		comboBoxModel = new EnumComboBoxModel<JBPopupFactory.ActionSelectionAid>(JBPopupFactory.ActionSelectionAid.class);
		comboBoxModel.setSelectedItem(settings.getPopupSelectionAid());
		popupAidComboBox.setModel(comboBoxModel);

		filterListModel = new DefaultListModel();
		for (String s : settings.getRecentProjectPaths()) {
			filterListModel.addElement(s);
		}
		recentProjectFiltersList.setModel(filterListModel);
		recentProjectFiltersList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		includeListModel = new DefaultListModel();
		for (String s : settings.getIncludeLocations()) {
			includeListModel.addElement(s);
		}
		includeProjectList.setModel(includeListModel);
		includeProjectList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	}

	private void browseForFile(DefaultListModel listModel) {
		final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor();

		descriptor.setTitle("Select parent folder");
		// 10.5 does not have #chooseFile
		VirtualFile[] virtualFile = FileChooser.chooseFiles(descriptor, null, null);
		if (virtualFile != null) {
			for (int i = 0; i < virtualFile.length; i++) {
				VirtualFile file = virtualFile[i];
				listModel.addElement(file.getPath());
			}
		}
	}

	public JPanel getRoot() {
		return root;
	}

	public void importFrom(FrameSwitcherSettings data) {
		initModel(data);
		setData(data);
		comboBoxModel.setSelectedItem(data.getPopupSelectionAid());
	}

	public FrameSwitcherSettings exportDisplayedSettings() {
		try {
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(maxRecentProjects.getText());
		} catch (Exception e) {
			maxRecentProjects.setText("");
		}
		getData(settings);
		settings.setPopupSelectionAid(comboBoxModel.getSelectedItem());
		settings.setRecentProjectPaths(toListStrings(filterListModel.toArray()));
		settings.setIncludeLocations(toListStrings(includeListModel.toArray()));
		return settings;
	}

	private List<String> toListStrings(final Object[] objects) {
		final ArrayList<String> recentProjectPaths = new ArrayList<String>();
		for (Object object : objects) {
			recentProjectPaths.add((String) object);
		}
		return recentProjectPaths;
	}


	public boolean isModified_custom(FrameSwitcherSettings data) {
		if (!Arrays.equals(filterListModel.toArray(), data.getRecentProjectPaths().toArray())) {
			return true;
		}
		if (!Arrays.equals(includeListModel.toArray(), data.getIncludeLocations().toArray())) {
			return true;
		}
		if (comboBoxModel.getSelectedItem() != data.getPopupSelectionAid()) {
			return true;
		}
		return isModified(data);
	}

	public void setData(FrameSwitcherSettings data) {
		maxRecentProjects.setText(data.getMaxRecentProjects());
		remoting.setSelected(data.isRemoting());
		defaultSelectionCurrentProject.setSelected(data.isDefaultSelectionCurrentProject());
		requestFocusMs.setText(data.getRequestFocusMs());
		loadProjectIcon.setSelected(data.isLoadProjectIcon());
	}

	public void getData(FrameSwitcherSettings data) {
		data.setMaxRecentProjects(maxRecentProjects.getText());
		data.setRemoting(remoting.isSelected());
		data.setDefaultSelectionCurrentProject(defaultSelectionCurrentProject.isSelected());
		data.setRequestFocusMs(requestFocusMs.getText());
		data.setLoadProjectIcon(loadProjectIcon.isSelected());
	}

	public boolean isModified(FrameSwitcherSettings data) {
		if (maxRecentProjects.getText() != null ? !maxRecentProjects.getText().equals(data.getMaxRecentProjects()) : data.getMaxRecentProjects() != null)
			return true;
		if (remoting.isSelected() != data.isRemoting()) return true;
		if (defaultSelectionCurrentProject.isSelected() != data.isDefaultSelectionCurrentProject()) return true;
		if (requestFocusMs.getText() != null ? !requestFocusMs.getText().equals(data.getRequestFocusMs()) : data.getRequestFocusMs() != null)
			return true;
		if (loadProjectIcon.isSelected() != data.isLoadProjectIcon()) return true;
		return false;
	}
}
