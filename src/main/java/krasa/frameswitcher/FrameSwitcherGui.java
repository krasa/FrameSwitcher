package krasa.frameswitcher;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
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

	private SlotsTableModel slotsTableModel;
	private JPanel wrapper;

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
		if (wrapper == null) {
			wrapper = new JPanel(new BorderLayout());
			wrapper.add(root, BorderLayout.NORTH);
			wrapper.add(buildSlotsPanel(), BorderLayout.CENTER);
		}
		return wrapper;
	}

	private JPanel buildSlotsPanel() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder("Frame Slots"));

		JLabel help = new JLabel("<html>Open the Frame Switcher popup (Alt+F2), highlight a frame, then press<br/>" +
				"<b>Ctrl+1</b>…<b>Ctrl+9</b> to toggle its slot assignment (these defaults apply only inside the popup).<br/>" +
				"To trigger <i>Switch to Frame Slot N</i> globally, bind it in Settings | Keymap under <i>Frame Slots</i>.</html>");
		panel.add(help, BorderLayout.NORTH);

		slotsTableModel = new SlotsTableModel(settings.getSlotToProjectPath());
		JTable table = new JTable(slotsTableModel);
		table.setRowHeight(table.getRowHeight() + 4);
		setColumnWidth(table, 0, 50, 50);
		setColumnWidth(table, 2, 140, 180);
		setColumnWidth(table, 3, 140, 180);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(600, 200));
		panel.add(scroll, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton clear = new JButton("Clear selected");
		clear.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				slotsTableModel.clearRow(row);
			}
		});
		JButton clearAll = new JButton("Clear all");
		clearAll.addActionListener(e -> slotsTableModel.clearAll());
		JButton openKeymap = new JButton("Configure shortcuts...");
		openKeymap.addActionListener(e -> {
			EditKeymapsDialog dialog = new EditKeymapsDialog(null, "krasa.frameswitcher.SwitchToFrameSlot1");
			ApplicationManager.getApplication().invokeLater(dialog::show);
		});
		buttons.add(clear);
		buttons.add(clearAll);
		buttons.add(openKeymap);
		panel.add(buttons, BorderLayout.SOUTH);

		return panel;
	}

	private static void setColumnWidth(JTable table, int idx, int preferred, int max) {
		TableColumn col = table.getColumnModel().getColumn(idx);
		col.setPreferredWidth(preferred);
		col.setMaxWidth(max);
	}

	private static class SlotsTableModel extends AbstractTableModel {
		private static final String[] COLS = {"Slot", "Project path", "Switch shortcut", "Assign shortcut"};
		private final Map<Integer, String> data = new LinkedHashMap<>();

		SlotsTableModel(Map<Integer, String> initial) {
			reset(initial);
		}

		void reset(Map<Integer, String> source) {
			data.clear();
			source.forEach((slot, path) -> {
				if (path != null && !path.isEmpty()) data.put(slot, path);
			});
			fireTableDataChanged();
		}

		Map<Integer, String> snapshot() {
			return new LinkedHashMap<>(data);
		}

		void clearRow(int row) {
			data.remove(row + 1);
			fireTableRowsUpdated(row, row);
		}

		void clearAll() {
			data.clear();
			fireTableDataChanged();
		}

		@Override public int getRowCount() { return FrameSwitcherSettings.SLOT_COUNT; }
		@Override public int getColumnCount() { return COLS.length; }
		@Override public String getColumnName(int c) { return COLS[c]; }
		@Override public boolean isCellEditable(int r, int c) { return c == 1; }

		@Override
		public Object getValueAt(int row, int col) {
			int slot = row + 1;
			switch (col) {
				case 0: return slot;
				case 1: return data.getOrDefault(slot, "");
				case 2: return shortcutFor(SwitchToFrameSlotAction.idFor(slot), null);
				case 3: return shortcutFor(AssignFrameSlotAction.idFor(slot), "Ctrl+" + slot + " (default)");
				default: return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if (col == 1) {
				String trimmed = value == null ? "" : value.toString().trim();
				if (trimmed.isEmpty()) {
					data.remove(row + 1);
				} else {
					data.put(row + 1, trimmed);
				}
				fireTableCellUpdated(row, col);
			}
		}

		private static String shortcutFor(String actionId, String fallback) {
			Shortcut[] shortcuts = KeymapManagerEx.getInstanceEx().getActiveKeymap().getShortcuts(actionId);
			if (shortcuts.length == 0) {
				return fallback != null ? fallback : "(unbound)";
			}
			for (Shortcut s : shortcuts) {
				if (s instanceof KeyboardShortcut) {
					return KeymapUtil.getShortcutText(s);
				}
			}
			return KeymapUtil.getShortcutText(shortcuts[0]);
		}
	}

	public void importFrom(FrameSwitcherSettings data) {
		initModel(data);
		setData(data);
		comboBoxModel.setSelectedItem(data.getPopupSelectionAid());
		if (slotsTableModel != null) {
			slotsTableModel.reset(data.getSlotToProjectPath());
		}
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
		if (slotsTableModel != null) {
			settings.setSlotToProjectPath(slotsTableModel.snapshot());
		}
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
		if (slotsTableModel != null && !slotsTableModel.snapshot().equals(data.getSlotToProjectPath())) {
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
