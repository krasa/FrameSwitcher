
package krasa.frameswitcher.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.intellij.openapi.diagnostic.Logger;
import de.ruedigermoeller.fastcast.config.FCConfigBuilder;
import de.ruedigermoeller.fastcast.util.FCLog;
import krasa.frameswitcher.remote.domain.OpenProject;
import krasa.frameswitcher.remote.domain.RemoteProject;
import krasa.frameswitcher.remote.domain.RemoteResult;

import com.intellij.openapi.project.Project;

import de.ruedigermoeller.fastcast.config.FCClusterConfig;
import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCRemoting;
import de.ruedigermoeller.fastcast.remoting.FastCast;

/**
 * @author Vojtech Krasa
 */
public class RemoteCommunicator {
	
	
	private static final Logger LOG= Logger.getInstance("#" + RemoteCommunicator .class.getName());
	protected final String uuid;
	protected Service service;
	protected Client client;
	protected volatile List<String> activeInstances = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		final RemoteCommunicator remoteCommunicator = new RemoteCommunicator();
		remoteCommunicator.ping();
		Thread.sleep(1000000);
	}

	public RemoteCommunicator() throws Exception {
		uuid = UUID.randomUUID().toString();
		final FCRemoting fcRemoting = initApp();
		client = new Client(uuid, fcRemoting);
//		service = new Service(client, uuid, fcRemoting) {
//			@Override
//			protected void activeInstance(String uuid) {
//				activeInstances.add(uuid);
//			}
//		};
	}

	public static FCClusterConfig getClusterConfig() {
		// configure a cluster programatically (for larger scale apps one should prefer config files)
		FCClusterConfig conf = FCConfigBuilder.New().sharedMemTransport("default", new File("C:/temp/mem1"),20,16000).topic("rpc",
				0).membership("members", 1) // built in topic for self monitoring+cluster view
		.end().build();

		conf.setLogLevel(FCLog.INFO);
		return conf;
	}


	public FCRemoting initApp() throws Exception {
		FCRemoting remoting = FastCast.getRemoting();

		// get the cluster config (only oone config per VM is possible)
		FCClusterConfig conf = getClusterConfig();

		// wire sockets/queues etc.
		remoting.joinCluster(conf, "Caller", null);

		// open topic for sending
		remoting.startSending("rpc", Service.class);

		// get proxy object to call remote methods (=send messages to other nodes)
		return remoting;

	}
	
	public List<String> getActiveInstances() {
		return activeInstances;
	}

	public void ping() {
		client.sendProxy.ping(System.currentTimeMillis(),uuid);
	}

	public void fetchRemoteProjects(final FCFutureResultHandler<RemoteResult> result) {
		client.sendProxy.getProjects(System.currentTimeMillis(), result);
	}

	public void openProject(OpenProject project) {
		client.sendProxy.openProject(project);
	}

	public void projectOpened(Project project) {
		client.sendProxy.projectOpened(uuid, new RemoteProject(project));
	}

	public void projectClosed(Project project) {
		client.sendProxy.projectClosed(uuid, new RemoteProject(project));
	}
}
