
package de.rc.branchwarner;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(
        name = "BranchWarnerSettings",
        storages = @Storage("branch-warner.xml")
)
public final class LiveBadgeSettings implements PersistentStateComponent<LiveBadgeSettings.State> {

	public static final class State {
		public String branches = "live,prod,production";
		public int scale = 1;
		public String corner = "BOTTOM_RIGHT";
	}

    private State state = new State();

    @Override
    public @Nullable State getState() { return state; }


	@Override
	public void loadState(@NotNull State state) {
		this.state = state;
		if (this.state.corner == null || this.state.corner.isBlank()) {
			this.state.corner = "BOTTOM_RIGHT";
		}
		if (this.state.scale < 1) {
			this.state.scale = 1;
		}
	}

	
	public @NotNull State getStateOrDefault() {
		if (state == null) {
			state = new State();
		}
		if (state.corner == null || state.corner.isBlank()) {
			state.corner = "BOTTOM_RIGHT";
		}
		if (state.scale < 1) {
			state.scale = 1;
		}
		return state;
	}
	
}
