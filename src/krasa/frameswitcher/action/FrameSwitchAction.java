package krasa.frameswitcher.action;

import com.google.common.collect.Multimap;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

import krasa.frameswitcher.FrameSwitcherApplicationComponent;
import krasa.frameswitcher.FrameSwitcherSettings;
import krasa.frameswitcher.remote.domain.OpenProject;
import krasa.frameswitcher.remote.RemoteCommunicator;
import krasa.frameswitcher.remote.domain.RemoteProject;

public class FrameSwitchAction extends QuickSwitchSchemeAction implements DumbAware {

	@Override
	protected void fillActions(final Project project, DefaultActionGroup group, DataContext dataContext) {
		final FrameSwitcherApplicationComponent applicationComponent = FrameSwitcherApplicationComponent.getInstance();
		final RemoteCommunicator remoteCommunicator = applicationComponent.getRemoteCommunicator();
		remoteCommunicator.ping();
		
		ArrayList<IdeFrame> list = getIdeFrames();
		for (final IdeFrame frame : list) {
			final Project project1 = frame.getProject();
			if (project1 != null) {
                Icon itemIcon = (project == project1) ? ourCurrentAction : ourNotCurrentAction;
                DumbAwareAction action = new DumbAwareAction(project1.getName(), null, itemIcon) {

					@Override
					public void actionPerformed(AnActionEvent e) {
						JComponent component = frame.getComponent();
						JFrame frame1 = WindowManager.getInstance().getFrame(project1);
						frame1.setVisible(true);
						frame1.setState(Frame.NORMAL);
						component.grabFocus();
					}
				};
				group.addAction(action);
			}
		}
		applicationComponent.sweepRemoteInstance();
		
		Multimap<String, RemoteProject> remoteProjectsMap = applicationComponent.getRemoteProjectMultimap();

		if (remoteProjectsMap.size() > 0) {
			group.addSeparator("RemoteProjects");
		}
		for (final String uuid : remoteProjectsMap.keySet()) {

			Collection<RemoteProject> remoteProjects = remoteProjectsMap.get(uuid);

			for (final RemoteProject remoteProject : remoteProjects) {
				final OpenProject openProject = new OpenProject(uuid, remoteProject);
				group.add(new DumbAwareAction(remoteProject.getName()) {
					@Override
					public void actionPerformed(AnActionEvent anActionEvent) {
						try {
							applicationComponent.getRemoteCommunicator().openProject(openProject);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
		final AnAction[] recentProjectsActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);
		if (recentProjectsActions != null) {
			FrameSwitcherSettings settings = FrameSwitcherSettings.getInstance();

			final int maxRecentProjectsAsInt = settings.getMaxRecentProjectsAsInt();
			int i = 0;
			for (AnAction recentProjectsAction : recentProjectsActions) {
				if (i >= maxRecentProjectsAsInt) {
					break;
				}
				if (settings.shouldShow((ReopenProjectAction) recentProjectsAction)) {
					if (i == 0) {
						group.addSeparator("Recent");
					}
					group.add(recentProjectsAction);
					i++;
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
