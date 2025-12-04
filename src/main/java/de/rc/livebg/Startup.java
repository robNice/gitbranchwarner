package de.rc.branchwarner;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class Startup implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        project.getService(BranchStateService.class);
        System.out.println("[BranchWarner] Startup -> service initialized for " + project.getName());
    }
}
