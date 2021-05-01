package krasa.frameswitcher;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

public class MyPreloadingActivity extends PreloadingActivity {
	@Override
	public void preload(@NotNull ProgressIndicator progressIndicator) {
		FrameSwitcherApplicationService.getInstance().initComponent();
	}
}
