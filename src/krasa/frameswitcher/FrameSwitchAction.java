package krasa.frameswitcher;

import com.google.common.collect.Multimap;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.ListPopupModel;
import krasa.frameswitcher.networking.dto.RemoteProject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

	private final Logger LOG = Logger.getInstance("#" + getClass().getCanonicalName());

	@Override
	protected void fillActions(final Project currentProject, DefaultActionGroup group, DataContext dataContext) {
		addFrames(currentProject, group);

		addRemote(group);

		addRecent(group);

		addRemoteRecent(group);
	}

	private void addFrames(Project currentProject, DefaultActionGroup group) {
		WindowManager windowManager = WindowManager.getInstance();
		ArrayList<IdeFrame> ideFrames = getIdeFrames();

		ProjectFocusMonitor projectFocusMonitor = FrameSwitcherApplicationComponent.getInstance().getProjectFocusMonitor();
		Project[] projectsOrderedByFocus = projectFocusMonitor.getProjectsOrderedByFocus();
		for (int i = projectsOrderedByFocus.length - 1; i >= 0; i--) {
			Project project = projectsOrderedByFocus[i];
			add(currentProject, group, project);

			IdeFrame frame = (IdeFrame) windowManager.getFrame(project);
			if (frame != null) {
				ideFrames.remove(frame);
			}
		}

		for (final IdeFrame frame : ideFrames) {
			final Project project = frame.getProject();
			if (project != null) {
				add(currentProject, group, project);
			}
		}
	}

	private void add(Project currentProject, DefaultActionGroup group, final Project project) {
		Icon itemIcon = (currentProject == project) ? ourCurrentAction : ourNotCurrentAction;
		DumbAwareAction action = new DumbAwareAction(project.getName().replace("_","__"), null, itemIcon) {

			@Override
			public void actionPerformed(AnActionEvent e) {
				FocusUtils.requestFocus(project, false);
			}
		};
		group.addAction(action);
	}

	private void addRemote(DefaultActionGroup group) {
		final FrameSwitcherApplicationComponent applicationComponent = FrameSwitcherApplicationComponent.getInstance();
		applicationComponent.getRemoteInstancesState().sweepRemoteInstance();
		Multimap<UUID, RemoteProject> remoteProjectMultimap = FrameSwitcherApplicationComponent.getInstance().getRemoteInstancesState().getRemoteProjects();
		applicationComponent.getRemoteSender().pingRemote();
		if (remoteProjectMultimap.size() > 0) {
			group.addSeparator("RemoteProjects");
		}
		for (final UUID uuid : remoteProjectMultimap.keySet()) {

			Collection<RemoteProject> remoteProjects = remoteProjectMultimap.get(uuid);

			for (final RemoteProject remoteProject : remoteProjects) {
				group.add(new DumbAwareAction(remoteProject.getName()) {

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

		final AnAction[] recentProjectsActions = recentProjectsManagerBase.getRecentProjectsActions(false);
		if (recentProjectsActions != null) {
			FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();

			int i = 0;
			for (AnAction action : recentProjectsActions) {
				ReopenProjectAction recentProjectsAction = (ReopenProjectAction) action;
				if (settings.shouldShow(recentProjectsAction)) {
					if (i == 0) {
						group.addSeparator("Recent");
					}
					group.add(recentProjectsAction);
					i++;
				}
			}
		}
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
						group.add(new ReopenProjectAction(remoteProject.getProjectPath(), remoteProject.getName(),
								remoteProject.getName()));
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
								if (invoked.get()) {
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
				popup.registerAction("invokeWithDelete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), new AbstractAction() {
					public void actionPerformed(ActionEvent e) {
						invoked.set(true);
						JList list = popup.getList();
						int selectedIndex = list.getSelectedIndex();
						ListPopupModel model = (ListPopupModel) list.getModel();
						PopupFactoryImpl.ActionItem selectedItem = (PopupFactoryImpl.ActionItem) model.get(selectedIndex);
						if (selectedItem != null && selectedItem.getAction() instanceof ReopenProjectAction) {
							ReopenProjectAction action = (ReopenProjectAction) selectedItem.getAction();
							FrameSwitcherUtils.getRecentProjectsManagerBase().removePath(action.getProjectPath());
							model.deleteItem(selectedItem);
							if (selectedIndex == list.getModel().getSize()) { //is last
								list.setSelectedIndex(selectedIndex - 1);
							} else {
								list.setSelectedIndex(selectedIndex);
							}
						}
					}
				});
			}
		}
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}

	protected JBPopupFactory.ActionSelectionAid getAidMethod() {
		return FrameSwitcherSettings.getInstance().getPopupSelectionAid();
	}

}
