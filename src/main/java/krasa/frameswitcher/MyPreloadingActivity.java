package krasa.frameswitcher;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class MyPreloadingActivity implements StartupActivity.DumbAware {

	@Override
	public void runActivity(@NotNull Project project) {
		FrameSwitcherApplicationService.getInstance(); //for initComponent()
	}
}
