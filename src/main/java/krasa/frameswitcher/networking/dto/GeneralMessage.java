package krasa.frameswitcher.networking.dto;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public abstract class GeneralMessage implements Serializable {
	protected UUID uuid;
	protected int version = 1;

	public GeneralMessage(UUID uuid) {
		this.uuid = uuid;
	}

	public int getVersion() {
		return version;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
