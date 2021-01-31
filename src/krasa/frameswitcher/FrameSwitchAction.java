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
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.WizardPopup;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.ListPopupModel;
import com.intellij.util.Alarm;
import com.intellij.util.PathUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SingleAlarm;
import com.intellij.util.ui.EmptyIcon;
import krasa.frameswitcher.networking.dto.RemoteProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

	private final static Logger LOG = Logger.getInstance(FrameSwitchAction.class);
	public static Icon forward;
	public static Icon empty;

	static {
		forward = AllIcons.Actions.Forward;
		empty = IconLoader.createLazy(() -> {
			return EmptyIcon.create(AllIcons.Actions.Forward.getIconWidth(), AllIcons.Actions.Forward.getIconHeight());
		});
	}

	private boolean loadProjectIcon;

	public FrameSwitchAction() {
	}

	@Override
	protected void fillActions(final Project currentProject, DefaultActionGroup group, DataContext dataContext) {
		loadProjectIcon = FrameSwitcherSettings.getInstance().isLoadProjectIcon();

		addFrames(currentProject, group);

		fillReopen(group);
	}

	public void fillReopen(DefaultActionGroup group) {
		addRemote(group);

		addRecent(group);

		addRemoteRecent(group);

		addIncluded(group);
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


		Condition<AnAction> preselectActionCondition;
		if (FrameSwitcherSettings.getInstance().isDefaultSelectionCurrentProject()) {
			preselectActionCondition = Conditions.alwaysFalse();
		} else {
			preselectActionCondition = (a) -> {
				if (a instanceof SwitchFrameAction) {
					return !((SwitchFrameAction) a).currentProject;
				}
				return true;
			};

		}

		ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
				getPopupTitle(e), group, e.getDataContext(), aid, true, null, -1,
				preselectActionCondition, myActionPlace);
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
		group.addAction(new SwitchFrameAction(project, currentProject == project));
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
				group.add(new SwitchToRemoteProjectAction(remoteProject, uuid));
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

	private void addIncluded(DefaultActionGroup group) {
		FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();
		List<String> includeLocations = settings.getIncludeLocations();
		if (includeLocations.isEmpty()) {
			return;
		}

		Set<String> paths = new HashSet<>();
		AnAction[] childActionsOrStubs = group.getChildActionsOrStubs();
		for (AnAction childActionsOrStub : childActionsOrStubs) {
			if (childActionsOrStub instanceof ReopenRecentWrapper) {
				paths.add(((ReopenRecentWrapper) childActionsOrStub).getProjectPath());
			} else if (childActionsOrStub instanceof SwitchToRemoteProjectAction) {
				paths.add(((SwitchToRemoteProjectAction) childActionsOrStub).getProjectPath());
			} else if (childActionsOrStub instanceof SwitchFrameAction) {
				paths.add(((SwitchFrameAction) childActionsOrStub).getBasePath());
			}

		}
		group.addSeparator("Included");

		for (String includeLocation : includeLocations) {
			File parent = new File(includeLocation);
			if (!parent.exists()) {
				continue;
			}
			File[] files = parent.listFiles();
			if (files == null) {
				continue;
			}
			for (File file : files) {
				if (file.isDirectory() && !file.getName().startsWith(".")) {
					String s = FileUtil.toSystemIndependentName(file.getAbsolutePath());
					if (paths.contains(s)) {
						continue;
					}
					group.add(new ReopenRecentWrapper(s));
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

						if (action.currentProject) {   //is actual
							if (selectedIndex == list.getModel().getSize()) { //is last
								selectedIndex--;
							}
							if (selectedIndex >= 0) {
								PopupFactoryImpl.ActionItem o = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
								if (o != null && o.getAction() instanceof SwitchFrameAction) {
									switchFrame(((SwitchFrameAction) o.getAction()).getProject());
								} else {
									selectedIndex--;
									o = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
									if (o != null && o.getAction() instanceof SwitchFrameAction) {
										switchFrame(((SwitchFrameAction) o.getAction()).getProject());
									}
								}
							}
						}
					}
				}
			}
		});
		Shortcut[] frameSwitchActions = KeymapManagerEx.getInstanceEx().getActiveKeymap().getShortcuts(getId());
		if (frameSwitchActions == null) {
			return;
		}
		hackField(popup);

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
						register(popup, KeyStroke.getKeyStroke("released " + s.toUpperCase()), new AbstractAction() {
							public void actionPerformed(ActionEvent e) {
								hackField(popup);
								if (invoked.get() || FrameSwitcherSettings.getInstance().isSelectImmediately()) {
									popup.handleSelect(true);
								}
							}
						});

						registerHack(popup, KeyStroke.getKeyStroke(s.toLowerCase() + " released SHIFT"));
					}
				}
				register(popup, keyboardShortcut.getFirstKeyStroke(), new AbstractAction() {
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
				registerHack(popup, KeyStroke.getKeyStroke(keyboardShortcut.getFirstKeyStroke().toString().replace("pressed", "released")));
				registerHack(popup, KeyStroke.getKeyStroke("shift " + keyboardShortcut.getFirstKeyStroke().toString().replace("pressed", "released")));
				register(popup, KeyStroke.getKeyStroke("shift " + keyboardShortcut.getFirstKeyStroke()), new AbstractAction() {
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

	private void registerHack(ListPopupImpl popup, KeyStroke keyStroke) {
		LOG.debug("registeringHACK ", keyStroke);
		if (popup != null) {
			popup.registerAction("Custom:" + keyStroke, keyStroke, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					hackField(popup);
				}
			});
		}
	}

	private void register(ListPopupImpl popup, KeyStroke keyStroke, AbstractAction action) {
		LOG.debug("registering ", keyStroke);
		if (keyStroke != null) {
			popup.registerAction("Custom:" + keyStroke, keyStroke, action);
		}
	}

	private void hackField(ListPopupImpl popup) {
		try {
			LOG.debug("hacking WizardPopup#myKeyPressedReceived");
			ReflectionUtil.setField(WizardPopup.class, popup, null, "myKeyPressedReceived", true);
		} catch (Throwable e) {
			//good
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

	private class ReopenRecentWrapper extends ReopenProjectAction {

		public ReopenRecentWrapper(ReopenProjectAction recentProjectsAction) {
			super(recentProjectsAction.getProjectPath(), recentProjectsAction.getProjectName(), recentProjectsAction.getTemplatePresentation().getText());
			getTemplatePresentation().setIcon(IconResolver.resolveIcon(recentProjectsAction.getProjectPath(), loadProjectIcon));
		}

		public ReopenRecentWrapper(String s) {
			super(s, null, s);
			getTemplatePresentation().setIcon(IconResolver.resolveIcon(s, loadProjectIcon));
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
					if (java.util.Objects.equals(p.getBasePath(), action.getProjectPath()) || java.util.Objects.equals(p.getProjectFilePath(), action.getProjectPath())) {
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
						switchFrame(openProject);

					}, ms, Alarm.ThreadToUse.SWING_THREAD, openProject);
					singleAlarm.request();
				}
			} catch (NumberFormatException e) {
			}
		}
	}

	private class SwitchFrameAction extends DumbAwareAction {

		private final Project project;
		private final boolean currentProject;

		public SwitchFrameAction(Project project, boolean currentProject) {
			super(project.getName().replace("_", "__"));
			this.project = project;
			this.currentProject = currentProject;
			if (loadProjectIcon) {
				getTemplatePresentation().setIcon(IconResolver.resolveIcon(project.getBasePath(), loadProjectIcon));
			} else {
				getTemplatePresentation().setIcon(currentProject ? forward : empty);
			}
		}

		public Project getProject() {
			return project;
		}

		public @SystemIndependent String getBasePath() {
			return project.getBasePath();
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			switchFrame(project);
		}
	}

	public void switchFrame(Project project) {
		SwingUtilities.invokeLater(() -> {
			IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
				FocusUtils.requestFocus(project, false, isCustom());
			});
		});
	}

	@NotNull
	protected String getId() {
		return "FrameSwitchAction";
	}

	private class SwitchToRemoteProjectAction extends DumbAwareAction {

		private final RemoteProject remoteProject;
		private final UUID uuid;

		public SwitchToRemoteProjectAction(RemoteProject remoteProject, UUID uuid) {
			super(remoteProject.getName().replace("_", "__"));
			this.remoteProject = remoteProject;
			this.uuid = uuid;
			getTemplatePresentation().setIcon(IconResolver.resolveIcon(remoteProject.getProjectPath(), loadProjectIcon));
		}

		public String getProjectPath() {
			return FileUtil.toSystemIndependentName(remoteProject.getProjectPath());
		}

		@Override
		public void actionPerformed(AnActionEvent anActionEvent) {
			FrameSwitcherApplicationComponent.getInstance().getRemoteSender().openProject(uuid, remoteProject);
		}
	}

	protected boolean isCustom() {
		return false;
	}
}
