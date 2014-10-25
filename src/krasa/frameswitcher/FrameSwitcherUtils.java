package krasa.frameswitcher;

import com.intellij.ide.RecentProjectsManagerBase;

/**
 * @author Vojtech Krasa
 */
public class FrameSwitcherUtils {
	public static RecentProjectsManagerBase getRecentProjectsManagerBase() {
		RecentProjectsManagerBase recentProjectsManagerBase = null;
		try {
			recentProjectsManagerBase = (RecentProjectsManagerBase) RecentProjectsManagerBase.class.getDeclaredMethod("getInstanceEx").invoke(null);
		} catch (Throwable e) {
			try {
				recentProjectsManagerBase = (RecentProjectsManagerBase) RecentProjectsManagerBase.class.getDeclaredMethod("getInstance").invoke(null);
			} catch (Throwable e1) {
				throw new RuntimeException(e1);
			}
		}
		return recentProjectsManagerBase;
	}
}
