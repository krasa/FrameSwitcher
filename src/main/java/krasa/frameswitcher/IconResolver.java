package krasa.frameswitcher;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IconResolver {
	private final static Logger LOG = Logger.getInstance(FrameSwitchAction.class);
	static Set<String> brokenIcons = new HashSet<>();
	static Map<String, Icon> cache = new HashMap<>();

	public static Icon resolveIcon(@SystemIndependent String basepath, boolean loadProjectIcon) {
		if (brokenIcons.contains(basepath)) {
			return null;
		}
		Icon cachedIcon = cache.get(basepath);
		if (cachedIcon != null) {
			return cachedIcon;
		}
		long start = System.currentTimeMillis();
		Icon icon = resolveIcon2(basepath, loadProjectIcon);
		long end = System.currentTimeMillis();
		if (end - start > 200) {
			brokenIcons.add(basepath);
			LOG.warn("Icon resolving took too long: " + (end - start) + "ms - " + basepath);
		}
		return icon;
	}

	@Nullable
	private static Icon resolveIcon2(String basepath, boolean loadProjectIcon) {
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
			icon = getIcon(base, ".idea/icon.png");
			if (icon != null) {
				return icon;
			}
			// icon = getIcon(base, ".idea/icon.svg");
			// if (icon != null) {
			// return icon;
			// }
			icon = getIcon(base, "src/main/resources/META-INF/pluginIcon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "resources/META-INF/pluginIcon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "META-INF/pluginIcon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "icon.png");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, "icon.svg");
			if (icon != null) {
				return icon;
			}
			icon = getIcon(base, ".idea/icon.svg");
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
	public static Icon getIcon(File base, String subpath) {
		File file = null;
		try {
			Icon icon = null;
			if (!JBColor.isBright()) {
				File darcula = new File(base, subpath.replace(".svg", "_dark.svg").replace(".png", "_dark.png"));
				if (darcula.exists()) {
					file = darcula;
				}
			}
			if (file == null) {
				file = new File(base, subpath);
			}

			if (file.exists()) {
				icon = new JBImageIcon(loadImage(file));
//				if (icon != null && icon.getIconHeight() > 1
//						&& icon.getIconHeight() != FrameSwitchAction.empty.getIconHeight()) {
//					icon = IconUtil.scale(icon, null,
//							(float) FrameSwitchAction.empty.getIconHeight() / icon.getIconHeight());
//				}
//				// material-theme-jetbrains needs to be scaled 2x
//				if (icon != null && icon.getIconHeight() > 1
//						&& icon.getIconHeight() != FrameSwitchAction.empty.getIconHeight()) {
//					icon = IconUtil.scale(icon, null,
//							(float) FrameSwitchAction.empty.getIconHeight() / icon.getIconHeight());
//				}
				if (icon.getIconHeight() > FrameSwitchAction.empty.getIconHeight()
						|| icon.getIconWidth() > FrameSwitchAction.empty.getIconWidth()) {
					brokenIcons.add(base.getAbsolutePath());
					LOG.warn("Scaling failed, wrong icon size: " + file.getAbsolutePath() + " " + icon.getIconHeight()
							+ "x" + icon.getIconWidth());
					return null;
				}
			}
			return icon;
		} catch (Throwable e) {
			LOG.warn(String.valueOf(file), e);
			return null;
		}
	}

	public static Image loadImage(File file) throws IOException {
		Image image = ImageLoader.loadFromUrl(file.toURI().toURL());
		if (image != null) {
			image = ImageLoader.scaleImage(image, FrameSwitchAction.empty.getIconHeight(), FrameSwitchAction.empty.getIconHeight());
		}
		return image;
//		if (file.getName().endsWith(".svg")) {
//			return SVGLoader.load(file.toURI().toURL(), 1.0f);
//		} else {
//			return ImageIO.read(file);
//		}
	}
}
