package de.rc.branchwarner;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class LiveBadgeSettingsConfigurable implements SearchableConfigurable {

    private final Project project;

    private JPanel panel;
    private JTextField branches;
    private JSpinner scale;
    private JComboBox<PositionOption> corner;

    private static final class PositionOption {
        final String key;
        final String label;

        PositionOption(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;  
        }
    }

    public LiveBadgeSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String getId() {
        return "de.rc.branchwarner.settings";
    }

    @Override
	public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Branch Warner";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());
            branches = new JTextField();
            scale = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
            corner = new JComboBox<>(new PositionOption[]{
                    new PositionOption("BOTTOM_RIGHT", "Bottom right (default)"),
                    new PositionOption("BOTTOM_LEFT",  "Bottom left"),
                    new PositionOption("TOP_RIGHT",    "Top right"),
                    new PositionOption("TOP_LEFT",     "Top left")
            });

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            int row = 0;

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Warn branches (comma separated):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(branches, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Badge size (1-5):"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(scale, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Badge position:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(corner, gbc);
        }

        reset();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public boolean isModified() {
        LiveBadgeSettings settings = project.getService(LiveBadgeSettings.class);
        LiveBadgeSettings.State s = settings.getStateOrDefault();

        String branchesText = branches.getText().trim();
        String stateBranches = (s.branches != null ? s.branches : "live,prod,production");
        if (!branchesText.equals(stateBranches)) {
            return true;
        }

        int uiScale = (Integer) scale.getValue();
        int stateScale = Math.max(1, s.scale);
        if (uiScale != stateScale) {
            return true;
        }

        PositionOption sel = (PositionOption) corner.getSelectedItem();
        String uiCornerKey = sel != null ? sel.key : "BOTTOM_RIGHT";

        String stateCorner = (s.corner != null && !s.corner.isBlank())
                ? s.corner
                : "BOTTOM_RIGHT";

        return !uiCornerKey.equalsIgnoreCase(stateCorner);
    }

    @Override
    public void apply() {
        LiveBadgeSettings settings = project.getService(LiveBadgeSettings.class);
        LiveBadgeSettings.State s = settings.getStateOrDefault();

        s.branches = branches.getText().trim();
        s.scale = Math.max(1, (Integer) scale.getValue());

        PositionOption sel = (PositionOption) corner.getSelectedItem();
        String key = (sel != null && sel.key != null && !sel.key.isBlank())
                ? sel.key
                : "BOTTOM_RIGHT";

        s.corner = key;
    }

    @Override
    public void reset() {
        LiveBadgeSettings settings = project.getService(LiveBadgeSettings.class);
        LiveBadgeSettings.State s = settings.getStateOrDefault();

        branches.setText(s.branches != null ? s.branches : "live,prod,production");
        scale.setValue(Math.max(1, s.scale));

        String stateCorner = (s.corner != null && !s.corner.isBlank())
                ? s.corner
                : "BOTTOM_RIGHT";

        PositionOption toSelect = null;
        for (int i = 0; i < corner.getItemCount(); i++) {
            PositionOption opt = corner.getItemAt(i);
            if (opt != null && opt.key.equalsIgnoreCase(stateCorner)) {
                toSelect = opt;
                break;
            }
        }
        if (toSelect == null && corner.getItemCount() > 0) {
            toSelect = corner.getItemAt(0);
        }
        corner.setSelectedItem(toSelect);
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        branches = null;
        scale = null;
        corner = null;
    }
}
