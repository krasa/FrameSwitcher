package krasa.frameswitcher.remote;

import com.intellij.openapi.diagnostic.Logger;
import de.ruedigermoeller.fastcast.config.FCClusterConfig;
import de.ruedigermoeller.fastcast.remoting.FCRemoting;
import de.ruedigermoeller.fastcast.remoting.FastCast;

public class Client {
	private static final Logger LOG= Logger.getInstance("#" + Client .class.getName());

	public Service sendProxy;
	private String uuid;

	public Client(String uuid, FCRemoting remoting) throws Exception {
		this.uuid = uuid;
		this.sendProxy = (Service) remoting.getRemoteService("rpc");;
	}

	public void pong(long timeStamp) {
		LOG.debug("pong");
		sendProxy.pong(timeStamp, uuid);
	}

}
