package krasa.frameswitcher;

import com.google.common.collect.Multimap;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import krasa.frameswitcher.networking.dto.RemoteProject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

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
		ArrayList<IdeFrame> list = getIdeFrames();
		for (final IdeFrame frame : list) {
			final Project project = frame.getProject();
			if (project != null) {
				Icon itemIcon = (currentProject == project) ? ourCurrentAction : ourNotCurrentAction;
				DumbAwareAction action = new DumbAwareAction(project.getName(), null, itemIcon) {

					@Override
					public void actionPerformed(AnActionEvent e) {
						FocusUtils.requestFocus(project);
					}
				};
				group.addAction(action);
			}
		}
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
		final AnAction[] recentProjectsActions = RecentProjectsManagerBase.getInstance().getRecentProjectsActions(false);
		if (recentProjectsActions != null) {
			FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();

			final int maxRecentProjectsAsInt = settings.getMaxRecentProjectsAsInt();
			int i = 0;
			for (AnAction action : recentProjectsActions) {
				ReopenProjectAction recentProjectsAction = (ReopenProjectAction) action;
				if (i >= maxRecentProjectsAsInt) {
					break;
				}
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
			final int maxRecentProjectsAsInt = settings.getMaxRecentProjectsAsInt() / entries.size();
			for (UUID entry : entries.toArray(new UUID[entries.size()])) {
				int i = 0;
				for (RemoteProject remoteProject : remoteRecentProjects.get(entry)) {
					if (i >= maxRecentProjectsAsInt) {
						break;
					}
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
	protected boolean isEnabled() {
		return true;
	}

	protected JBPopupFactory.ActionSelectionAid getAidMethod() {
		return FrameSwitcherSettings.getInstance().getPopupSelectionAid();
	}

}
