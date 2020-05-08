package krasa.frameswitcher;

import com.google.common.collect.Multimap;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.GeneralSettings;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.ListPopupModel;
import com.intellij.util.Alarm;
import com.intellij.util.PathUtil;
import com.intellij.util.SingleAlarm;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

	private final static Logger LOG = Logger.getInstance(FrameSwitchAction.class);
	private Icon forward = AllIcons.Actions.Forward;

	@Override
	protected void fillActions(final Project currentProject, DefaultActionGroup group, DataContext dataContext) {
		addFrames(currentProject, group);

		addRemote(group);

		addRecent(group);

		addRemoteRecent(group);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		DefaultActionGroup group = new DefaultActionGroup();
		fillActions(project, group, e.getDataContext());
		showPopup(e, group);
	}

	private void showPopup(AnActionEvent e, DefaultActionGroup group) {
		if (group.getChildrenCount() == 0) return;
		JBPopupFactory.ActionSelectionAid aid = getAidMethod();


		Condition<AnAction> condition;
		if (FrameSwitcherSettings.getInstance().isDefaultSelectionCurrentProject()) {
			condition = Conditions.alwaysFalse();
		} else {
			condition = (a) -> a.getTemplatePresentation().getIcon() != forward;
		}

		ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
			getPopupTitle(e), group, e.getDataContext(), aid, true, null, -1,
			condition, myActionPlace);
		showPopup(e, popup);
	}


	private void addFrames(Project currentProject, DefaultActionGroup group) {
		WindowManager windowManager = WindowManager.getInstance();
		ArrayList<IdeFrame> ideFrames = getIdeFrames();

		ProjectFocusMonitor projectFocusMonitor = FrameSwitcherApplicationComponent.getInstance().getProjectFocusMonitor();
		Project[] projectsOrderedByFocus = projectFocusMonitor.getProjectsOrderedByFocus();
		Set<Project> addedProjectsSet = new HashSet<>();

		for (int i = projectsOrderedByFocus.length - 1; i >= 0; i--) {
			Project project = projectsOrderedByFocus[i];
			if (project.isDisposed()) {
				continue;
			}
			add(currentProject, group, project);

			IdeFrame frame = (IdeFrame) windowManager.getFrame(project);
			if (frame != null) {
				addedProjectsSet.add(project);
			}
		}

		for (final IdeFrame frame : ideFrames) {
			final Project project = frame.getProject();
			if (project != null) {
				if (project.isDisposed()) {
					continue;
				}
				if (addedProjectsSet.contains(project)) {
					continue;
				}
				add(currentProject, group, project);
			}
		}
	}

	private void add(Project currentProject, DefaultActionGroup group, final Project project) {
		Icon itemIcon = (currentProject == project) ? forward : ourNotCurrentAction;
		group.addAction(new SwitchFrameAction(project, itemIcon));
	}

	private void addRemote(DefaultActionGroup group) {
		final FrameSwitcherApplicationComponent applicationComponent = FrameSwitcherApplicationComponent.getInstance();
		applicationComponent.getRemoteInstancesState().sweepRemoteInstance();
		Multimap<UUID, RemoteProject> remoteProjectMultimap = applicationComponent.getRemoteInstancesState().getRemoteProjects();
		applicationComponent.getRemoteSender().pingRemote();
		if (remoteProjectMultimap.size() > 0) {
			group.addSeparator("RemoteProjects");
		}
		for (final UUID uuid : remoteProjectMultimap.keySet()) {

			Collection<RemoteProject> remoteProjects = remoteProjectMultimap.get(uuid);

			for (final RemoteProject remoteProject : remoteProjects) {
				group.add(new DumbAwareAction(remoteProject.getName().replace("_", "__")) {

					@Override
					public void actionPerformed(AnActionEvent anActionEvent) {
						FrameSwitcherApplicationComponent.getInstance().getRemoteSender().openProject(uuid, remoteProject);
					}
				});
			}
		}
	}

	private void addRecent(DefaultActionGroup group) {
		RecentProjectsManagerBase recentProjectsManagerBase = FrameSwitcherUtils.getRecentProjectsManagerBase();
		AnAction[] recentProjectsActions = recentProjectsManagerBase.getRecentProjectsActions(false, false);
		if (recentProjectsActions != null) {
			recentProjectsActions = removeCurrentProjects(recentProjectsActions);
			FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();

			int i = 0;
			for (AnAction action : recentProjectsActions) {
				ReopenProjectAction recentProjectsAction = (ReopenProjectAction) action;
				if (settings.shouldShow(recentProjectsAction)) {
					if (i == 0) {
						group.addSeparator("Recent");
					}
					group.add(new ReopenRecentWrapper(recentProjectsAction));
					i++;
				}
			}
		}
	}

	public static AnAction[] removeCurrentProjects(AnAction[] actions) {
		ProjectFocusMonitor projectFocusMonitor = FrameSwitcherApplicationComponent.getInstance().getProjectFocusMonitor();
		Project[] projectsOrderedByFocus = projectFocusMonitor.getProjectsOrderedByFocus();


		if (projectsOrderedByFocus != null) {
			return Arrays.stream(actions)
				.filter(action -> {
					return !(action instanceof ReopenProjectAction)
						|| !isOpen(projectsOrderedByFocus, (ReopenProjectAction) action);
				})
				.toArray(AnAction[]::new);
		}
		return actions;
	}

	private static boolean isOpen(Project[] openedProjects, ReopenProjectAction action) {
		String projectPath = PathUtil.toSystemDependentName(action.getProjectPath());
		for (Project openedProject : openedProjects) {
			if (StringUtil.equals(projectPath, PathUtil.toSystemDependentName(openedProject.getBasePath())) || Objects.equals(PathUtil.toSystemDependentName(openedProject.getProjectFilePath()), projectPath)) {
				return true;
			}
		}
		return false;
	}

	private void addRemoteRecent(DefaultActionGroup group) {
		Multimap<UUID, RemoteProject> remoteRecentProjects = FrameSwitcherApplicationComponent.getInstance().getRemoteInstancesState().getRemoteRecentProjects();
		Set<UUID> entries = remoteRecentProjects.keySet();
		boolean addedSeparator = false;
		if (remoteRecentProjects.size() > 0) {
			FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();
			for (UUID entry : entries.toArray(new UUID[entries.size()])) {
				int i = 0;
				for (RemoteProject remoteProject : remoteRecentProjects.get(entry)) {
					if (settings.shouldShow(remoteProject)) {
						if (!addedSeparator) {
							addedSeparator = true;
							group.addSeparator("Remote recent");
						}
						group.add(new ReopenRecentWrapper(new ReopenProjectAction(remoteProject.getProjectPath(), remoteProject.getName(),
							remoteProject.getName())));
						i++;
					}
				}

			}
		}
	}

	public ArrayList<IdeFrame> getIdeFrames() {
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
				return project1.getName().compareToIgnoreCase(project2.getName());
			}
		});
		return list;
	}

	@Override
	protected void showPopup(AnActionEvent e, ListPopup p) {
		final ListPopupImpl popup = (ListPopupImpl) p;
		registerActions(popup);
		super.showPopup(e, popup);
	}


	private void registerActions(final ListPopupImpl popup) {
		final Ref<Boolean> invoked = Ref.create(false);
		popup.registerAction("ReopenInSameWindow", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JList list = popup.getList();
				PopupFactoryImpl.ActionItem selectedValue = (PopupFactoryImpl.ActionItem) list.getSelectedValue();
				if (selectedValue.getAction() instanceof ReopenRecentWrapper) {
					popup.closeOk(null);
					int confirmOpenNewProject = GeneralSettings.getInstance().getConfirmOpenNewProject();
					try {
						GeneralSettings.getInstance().setConfirmOpenNewProject(GeneralSettings.OPEN_PROJECT_SAME_WINDOW);
						ReopenRecentWrapper action = (ReopenRecentWrapper) selectedValue.getAction();
						action.actionPerformed(new AnActionEvent(null, getDataContext(popup), myActionPlace, getTemplatePresentation(), ActionManager.getInstance(), 0));
					} finally {
						GeneralSettings.getInstance().setConfirmOpenNewProject(confirmOpenNewProject);
					}
				}
			}
		});
		popup.registerAction("ReopenInNewWindow", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JList list = popup.getList();
				PopupFactoryImpl.ActionItem selectedValue = (PopupFactoryImpl.ActionItem) list.getSelectedValue();
				if (selectedValue.getAction() instanceof ReopenRecentWrapper) {
					popup.closeOk(null);
					int confirmOpenNewProject = GeneralSettings.getInstance().getConfirmOpenNewProject();
					try {
						GeneralSettings.getInstance().setConfirmOpenNewProject(GeneralSettings.OPEN_PROJECT_NEW_WINDOW);
						ReopenRecentWrapper action = (ReopenRecentWrapper) selectedValue.getAction();
						action.actionPerformed(new AnActionEvent(null, getDataContext(popup), myActionPlace, getTemplatePresentation(), ActionManager.getInstance(), 0));
					} finally {
						GeneralSettings.getInstance().setConfirmOpenNewProject(confirmOpenNewProject);
					}
				}
			}

		});
		popup.registerAction("invokeWithDelete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				invoked.set(true);
				JList list = popup.getList();
				int selectedIndex = list.getSelectedIndex();
				ListPopupModel model = (ListPopupModel) list.getModel();
				PopupFactoryImpl.ActionItem selectedItem = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
				if (selectedItem != null && selectedItem.getAction() instanceof ReopenRecentWrapper) {
					ReopenRecentWrapper action = (ReopenRecentWrapper) selectedItem.getAction();
					FrameSwitcherUtils.getRecentProjectsManagerBase().removePath(action.getProjectPath());
					model.deleteItem(selectedItem);
					if (selectedIndex == list.getModel().getSize()) { //is last
						list.setSelectedIndex(selectedIndex - 1);
					} else {
						list.setSelectedIndex(selectedIndex);
					}
				}
				if (selectedItem != null && selectedItem.getAction() instanceof SwitchFrameAction) {
					SwitchFrameAction action = (SwitchFrameAction) selectedItem.getAction();
					Project project = action.getProject();
					if (!project.isDisposed()) {
						ProjectUtil.closeAndDispose(project);
						FrameSwitcherUtils.getRecentProjectsManagerBase().updateLastProjectPath();
						WelcomeFrame.showIfNoProjectOpened();
						model.deleteItem(selectedItem);
						if (selectedIndex == list.getModel().getSize()) { //is last
							list.setSelectedIndex(selectedIndex - 1);
						} else {
							list.setSelectedIndex(selectedIndex);
						}

						if (action.getItemIcon() == AllIcons.Actions.Forward) {   //is actual
							if (selectedIndex == list.getModel().getSize()) { //is last
								selectedIndex--;
							}
							if (selectedIndex >= 0) {
								PopupFactoryImpl.ActionItem o = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
								if (o != null && o.getAction() instanceof SwitchFrameAction) {
									SwitchFrameAction.switchFrame(((SwitchFrameAction) o.getAction()).getProject());
								} else {
									selectedIndex--;
									o = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
									if (o != null && o.getAction() instanceof SwitchFrameAction) {
										SwitchFrameAction.switchFrame(((SwitchFrameAction) o.getAction()).getProject());
									}
								}
							}
						}
					}
				}
			}
		});
		Shortcut[] frameSwitchActions = KeymapManagerEx.getInstanceEx().getActiveKeymap().getShortcuts("FrameSwitchAction");
		if (frameSwitchActions == null) {
			return;
		}
		for (Shortcut switchAction : frameSwitchActions) {
			if (switchAction instanceof KeyboardShortcut) {
				KeyboardShortcut keyboardShortcut = (KeyboardShortcut) switchAction;
				String[] split = keyboardShortcut.getFirstKeyStroke().toString().split(" ");
				for (String s : split) {
					if (s.equalsIgnoreCase("alt")
						|| s.equalsIgnoreCase("ctrl")
						|| s.equalsIgnoreCase("meta")
					) {
						if (s.equalsIgnoreCase("ctrl")) {
							s = "control";
						}
						popup.registerAction(s + "Released", KeyStroke.getKeyStroke("released " + s.toUpperCase()), new AbstractAction() {
							public void actionPerformed(ActionEvent e) {
								if (invoked.get() || FrameSwitcherSettings.getInstance().isSelectImmediately()) {
									popup.handleSelect(true);
								}
							}
						});
					}
				}
				popup.registerAction("invoke", keyboardShortcut.getFirstKeyStroke(), new AbstractAction() {
					public void actionPerformed(ActionEvent e) {
						invoked.set(true);
						JList list = popup.getList();
						int selectedIndex = list.getSelectedIndex();
						int size = list.getModel().getSize();
						if (selectedIndex + 1 < size) {
							list.setSelectedIndex(selectedIndex + 1);
						} else {
							list.setSelectedIndex(0);
						}
					}
				});
				popup.registerAction("invokeWithShift", KeyStroke.getKeyStroke("shift " + keyboardShortcut.getFirstKeyStroke()), new AbstractAction() {
					public void actionPerformed(ActionEvent e) {
						invoked.set(true);
						JList list = popup.getList();
						int selectedIndex = list.getSelectedIndex();
						int size = list.getModel().getSize();
						if (selectedIndex - 1 >= 0) {
							list.setSelectedIndex(selectedIndex - 1);
						} else {
							list.setSelectedIndex(size - 1);
						}
					}
				});
			}
		}
	}

	private DataContext getDataContext(ListPopupImpl popup) {
		DataContext dataContext = DataManager.getInstance().getDataContext(popup.getOwner());
		Project project = dataContext.getData(CommonDataKeys.PROJECT);
		if (project == null) {
			throw new IllegalStateException("Project is null for " + popup.getOwner());
		}
		return dataContext;
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}

	protected JBPopupFactory.ActionSelectionAid getAidMethod() {
		return FrameSwitcherSettings.getInstance().getPopupSelectionAid();
	}

	private static class ReopenRecentWrapper extends ReopenProjectAction {

		public ReopenRecentWrapper(ReopenProjectAction recentProjectsAction) {
			super(recentProjectsAction.getProjectPath(), recentProjectsAction.getProjectName(), recentProjectsAction.getTemplatePresentation().getText());
		}

		@Override
		public void actionPerformed(AnActionEvent anActionEvent) {
			super.actionPerformed(anActionEvent);
			SwingUtilities.invokeLater(() -> {
				requestFocus(this);
			});
		}

		private void requestFocus(ReopenRecentWrapper action) {
			Project openedProject = null;
			Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
			for (int i = openProjects.length - 1; i >= 0; i--) {
				Project project = openProjects[i];
				if (project instanceof ProjectImpl) {
					ProjectImpl p = (ProjectImpl) project;
					if (Objects.equals(p.getBasePath(), action.getProjectPath()) || Objects.equals(p.getProjectFilePath(), action.getProjectPath())) {
						openedProject = project;
						break;
					}
				}
			}
			if (openedProject != null) {
				requestFocus(openedProject);
			} else {
				LOG.info("Unable to request focus for reopened project: " + action.getProjectPath());
			}
		}

		private void requestFocus(Project openProject) {
			String requestFocusMs = FrameSwitcherApplicationComponent.getInstance().getState().getRequestFocusMs();
			try {
				int ms = Integer.parseInt(requestFocusMs);
				if (ms > 0) {
					SingleAlarm singleAlarm = new SingleAlarm(() -> {
						if (openProject.isDisposed()) {
							return;
						}
						LOG.info("Requesting focus for " + openProject);
						FocusUtils.requestFocus(openProject, false);

					}, ms, Alarm.ThreadToUse.SWING_THREAD, openProject);
					singleAlarm.request();
				}
			} catch (NumberFormatException e) {
			}
		}
	}

	private static class SwitchFrameAction extends DumbAwareAction {

		private final Project project;
		private final Icon itemIcon;

		public SwitchFrameAction(Project project, Icon itemIcon) {
			super(project.getName().replace("_", "__"), null, itemIcon);
			this.project = project;
			this.itemIcon = itemIcon;
		}

		public Icon getItemIcon() {
			return itemIcon;
		}

		public Project getProject() {
			return project;
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			switchFrame(project);
		}

		public static void switchFrame(Project project) {
			SwingUtilities.invokeLater(() -> {
				IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
					FocusUtils.requestFocus(project, false);
				});
			});
		}
	}
}
