package krasa.frameswitcher;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.IconUtil;
import com.intellij.util.SVGLoader;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class IconResolver {
	private final static Logger LOG = Logger.getInstance(FrameSwitchAction.class);

	public static Icon resolveIcon(@SystemIndependent String basepath, boolean loadProjectIcon) {
		try {
			if (!loadProjectIcon) {
				return null;
			}
			File base = new File(basepath);
			if (!base.exists()) {
				return null;
			}
			if (base.isFile()) {
				base = base.getParentFile();
			}
			if (base.getName().startsWith(".")) {
				base = base.getParentFile();
			}
			if (!base.exists()) {
				return null;
			}


			Icon icon = null;
			icon = getIcon(base, "icon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "icon.png");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "resources\\META-INF\\pluginIcon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "META-INF\\pluginIcon.svg");
			if (icon != null) {
				return icon;
			}
			return icon;
		} catch (Throwable e) {
			LOG.debug(e);
			return null;
		}
	}

	@Nullable
	public static Icon getIcon(File base, String child) {
		try {
			Icon icon = null;
			File file = new File(base, child);
			if (file.exists()) {
				icon = new JBImageIcon(loadImage(file));
				if (icon != null && icon.getIconHeight() > 1 && icon.getIconHeight() != FrameSwitchAction.empty.getIconHeight()) {
					icon = IconUtil.scale(icon, null, (float) FrameSwitchAction.empty.getIconHeight() / icon.getIconHeight());
				}
			}
			return icon;
		} catch (Throwable e) {
			LOG.debug(e);
			return null;
		}
	}

	public static Image loadImage(File file) throws IOException {
		if (file.getName().endsWith(".svg")) {
			return SVGLoader.load(file.toURI().toURL(), 1.0f);
		} else {
			return ImageIO.read(file);
		}
	}
}