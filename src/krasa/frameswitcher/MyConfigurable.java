package krasa.frameswitcher;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MyConfigurable implements Configurable {
	private FrameSwitcherGui gui;
	@Nls
	@Override
	public String getDisplayName() {
		return "Frame Switcher";
	}

	@Nullable
	public Icon getIcon() {
		return null;
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		if (gui == null) {
			gui = new FrameSwitcherGui(getSettings());
		}
		return gui.getRoot();
	}

	@Override
	public boolean isModified() {
		return gui.isModifiedCustom(getSettings());
	}

	@Override
	public void apply() throws ConfigurationException {
		FrameSwitcherSettings settings = gui.exportDisplayedSettings();

		FrameSwitcherApplicationComponent component = FrameSwitcherApplicationComponent.getInstance();
		component.updateSettings(settings);
	}

	@Override
	public void reset() {
		gui.importFrom(getSettings());
	}

	@NotNull
	private FrameSwitcherSettings getSettings() {
		return FrameSwitcherApplicationComponent.getInstance().getState();
	}

	@Override
	public void disposeUIResources() {
		gui = null;
	}
}
