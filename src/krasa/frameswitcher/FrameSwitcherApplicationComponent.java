package krasa.frameswitcher;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ExportableApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

@State(name = "FrameSwitcherSettings", storages = {@Storage(id = "FrameSwitcherSettings", file = "$APP_CONFIG$/FrameSwitcherSettings.xml")})
public class FrameSwitcherApplicationComponent implements ApplicationComponent,
		PersistentStateComponent<FrameSwitcherSettings>, ExportableApplicationComponent, Configurable {

	private FrameSwitcherSettings settings;
	private FrameSwitcherGui gui;

	public FrameSwitcherApplicationComponent() {
	}

	public void initComponent() {
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "FrameSwitcherApplicationComponent";
	}

	@NotNull
	@Override
	public File[] getExportFiles() {
		return new File[]{PathManager.getOptionsFile("FrameSwitcherSettings")};
	}

	@NotNull
	@Override
	public String getPresentableName() {
		return "FrameSwitcher";
	}

	@Nullable
	@Override
	public FrameSwitcherSettings getState() {
		if (settings == null) {
			settings = new FrameSwitcherSettings();
		}
		return settings;
	}

	@Override
	public void loadState(FrameSwitcherSettings frameSwitcherSettings) {
		this.settings = frameSwitcherSettings;
	}

	// Configurable---------------------------------
	@Nls
	@Override
	public String getDisplayName() {
		return "FrameSwitcher";
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
			gui = new FrameSwitcherGui(getState());
		}
		return gui.getRoot();
	}

	@Override
	public boolean isModified() {
		return gui.isModified(getState());
	}

	@Override
	public void apply() throws ConfigurationException {
		settings = gui.exportDisplayedSettings();
	}

	@Override
	public void reset() {
		gui.importFrom(settings);
	}

	@Override
	public void disposeUIResources() {
		gui = null;
	}
}
