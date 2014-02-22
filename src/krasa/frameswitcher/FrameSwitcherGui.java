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

	private DefaultListModel listModel;
	private JPanel root;
	private JTextField maxRecentProjects;
	private JComboBox popupAidComboBox;
	private JList recentProjectFiltersList;
	private JButton addButton;
	private JButton remove;
	private JCheckBox remoting;

	private FrameSwitcherSettings settings;
	private EnumComboBoxModel<JBPopupFactory.ActionSelectionAid> comboBoxModel;

	public FrameSwitcherGui(FrameSwitcherSettings settings) {
		this.settings = settings;

		addButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				browseForFile();
			}
		});
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int leadSelectionIndex = recentProjectFiltersList.getSelectionModel().getLeadSelectionIndex();
				if (!recentProjectFiltersList.getSelectionModel().isSelectionEmpty()) {
					listModel.remove(leadSelectionIndex);
				}
			}
		});
		initModel(settings);
	}

	private void initModel(FrameSwitcherSettings settings) {
		comboBoxModel = new EnumComboBoxModel<JBPopupFactory.ActionSelectionAid>(JBPopupFactory.ActionSelectionAid.class);
		comboBoxModel.setSelectedItem(settings.getPopupSelectionAid());
		popupAidComboBox.setModel(comboBoxModel);
		listModel = new DefaultListModel();
		for (String s : settings.getRecentProjectPaths()) {
			listModel.addElement(s);
		}
		recentProjectFiltersList.setModel(listModel);
		recentProjectFiltersList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	private void browseForFile() {
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
		getData(settings);
		settings.setPopupSelectionAid(comboBoxModel.getSelectedItem());
		settings.setRecentProjectPaths(toListStrings(listModel.toArray()));
		return settings;
	}

	private List<String> toListStrings(final Object[] objects) {
		final ArrayList<String> recentProjectPaths = new ArrayList<String>();
		for (Object object : objects) {
			recentProjectPaths.add((String) object);
		}
		return recentProjectPaths;
	}


	public void setData(FrameSwitcherSettings data) {
		maxRecentProjects.setText(data.getMaxRecentProjects());
		remoting.setSelected(data.isRemoting());
	}

	public void getData(FrameSwitcherSettings data) {
		data.setMaxRecentProjects(maxRecentProjects.getText());
		data.setRemoting(remoting.isSelected());
	}

	public boolean isModified(FrameSwitcherSettings data) {
		//ALWAYS MUST BE HERE
		if (isModifiedCustom(data)) {
			return true;
		}
		if (maxRecentProjects.getText() != null ? !maxRecentProjects.getText().equals(data.getMaxRecentProjects()) : data.getMaxRecentProjects() != null)
			return true;
		if (remoting.isSelected() != data.isRemoting()) return true;
		return false;
	}

	private boolean isModifiedCustom(FrameSwitcherSettings data) {
		if (!Arrays.equals(listModel.toArray(), data.getRecentProjectPaths().toArray())) {
			return true;
		}
		if (comboBoxModel.getSelectedItem() != data.getPopupSelectionAid()) {
			return true;
		}
		return false;
	}
}
