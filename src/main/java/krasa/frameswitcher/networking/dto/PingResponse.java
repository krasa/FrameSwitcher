package krasa.frameswitcher.networking.dto;

import java.util.UUID;

public class PingResponse extends GeneralMessage {
	public PingResponse(UUID uuid) {
		super(uuid);
	}
}
