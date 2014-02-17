package krasa.frameswitcher.remote;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.UIUtil;
import krasa.frameswitcher.FrameSwitcherApplicationComponent;
import krasa.frameswitcher.action.FrameSwitchAction;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;

import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCRemoting;
import de.ruedigermoeller.fastcast.remoting.FCTopicService;
import de.ruedigermoeller.fastcast.remoting.RemoteMethod;
import krasa.frameswitcher.remote.domain.OpenProject;
import krasa.frameswitcher.remote.domain.RemoteProject;
import krasa.frameswitcher.remote.domain.RemoteResult;

/**
 * Created with IntelliJ IDEA. User: moelrue Date: 10/14/13 Time: 3:21 PM To change this template use File | Settings |
 * File Templates.
 * <p/>
 * To avoid boilerplate, no interface for the remote methods is defined. Also note that one will probably use a separate
 * Node main class setting up and managing several topics in case.
 */
public class Service extends FCTopicService {
	private static final Logger LOG= Logger.getInstance("#" + Service .class.getName());

	private Client client;
	private String uuid;

	// empty constructor required for bytecode magic
	public Service() {
	}

	public Service(Client client, String uuid, FCRemoting fcRemoting) throws Exception {
		fcRemoting.startReceiving("rpc", this);
		this.uuid = uuid;
		this.client = client;
	}

	@Override
	public void init() {
		// called by FastCast prior to receiving the first message
	}

	@RemoteMethod(1)
	public void ping(long timeStamp, String uuid) {
		System.out.println(Thread.currentThread().getName() + " ping received, roundtrip time "
				+ (System.currentTimeMillis() - timeStamp));
		client.pong(timeStamp);
		activeInstance(uuid);
	}

	@RemoteMethod(2)
	public void pong(long timeStamp, String uuid) {
		System.out.println(Thread.currentThread().getName() + " pong received, total roundtrip time "
				+ (System.currentTimeMillis() - timeStamp));
		LOG.debug("pong received,uuid="+uuid);
		activeInstance(uuid);
	}

	protected void activeInstance(String uuid) {
	}

	@RemoteMethod(3)
	public void openProject(OpenProject openProject) {
		LOG.debug("openProject "+openProject);
		if (openProject.getUuid().equals(uuid)) {
			ArrayList<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
			for (final IdeFrame frame : ideFrames) {
				final Project project = frame.getProject();
				if (project != null && openProject.getProject().getBasePath().equals(project.getBasePath())) {
					//execute on EDT
					UIUtil.invokeAndWaitIfNeeded(new Runnable() {
						@Override
						public void run() {
							JComponent component = frame.getComponent();
							JFrame frame1 = WindowManager.getInstance().getFrame(project);
							frame1.setVisible(true);
							frame1.setState(Frame.NORMAL);
							component.grabFocus();
							component.requestFocus();
						}
					});
				}
			}
		}
	}

	
	@RemoteMethod(7)
	public void getProjects(long timeStamp, FCFutureResultHandler<RemoteResult> result) {
		LOG.debug("getProjects");
		ArrayList<IdeFrame> ideFrames = new FrameSwitchAction().getIdeFrames();
		final ArrayList<RemoteProject> remoteProjects = new ArrayList<RemoteProject>();
		for (IdeFrame ideFrame : ideFrames) {
			remoteProjects.add(new RemoteProject(ideFrame.getProject()));
		}
		result.sendResult(new RemoteResult(timeStamp, uuid, remoteProjects));
		LOG.debug("getProjects done");
	}

	@RemoteMethod(8)
	public void projectOpened(String uuid, RemoteProject remoteProject) {
		LOG.debug("projectOpened "+uuid+ " "+remoteProject);
		FrameSwitcherApplicationComponent.getInstance().remoteProjectOpened(uuid, remoteProject);

	}

	@RemoteMethod(9)
	public void projectClosed(String uuid, RemoteProject remoteProject) {
		LOG.debug("projectClosed "+uuid+ " "+remoteProject);
		FrameSwitcherApplicationComponent.getInstance().projectClosed(uuid, remoteProject);
	}

}
