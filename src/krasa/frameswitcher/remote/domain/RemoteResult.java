package krasa.frameswitcher.remote.domain;

import java.io.Serializable;
import java.util.ArrayList;

/**
* @author Vojtech Krasa
*/
public class RemoteResult implements Serializable{
	private final long timeStamp;
	private final String uuid;
	private final ArrayList<RemoteProject> remoteProjects;

	public RemoteResult(long timeStamp, String uuid, ArrayList<RemoteProject> remoteProjects) {
		this.timeStamp = timeStamp;
		this.uuid = uuid;
		this.remoteProjects = remoteProjects;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getUuid() {
		return uuid;
	}

	public ArrayList<RemoteProject> getRemoteProjects() {
		return remoteProjects;
	}
}
