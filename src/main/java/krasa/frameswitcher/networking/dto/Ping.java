package krasa.frameswitcher.networking.dto;

import java.util.UUID;

public class Ping extends GeneralMessage {

	public Ping(UUID uuid) {
		super(uuid);
	}
}
