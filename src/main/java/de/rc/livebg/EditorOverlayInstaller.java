package de.rc.branchwarner;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;

import javax.swing.*;
import java.awt.*;

public final class EditorOverlayInstaller implements EditorFactoryListener {

    static final String KEY_CORNER_BADGE = "rc.live.cornerBadge";

    @Override
    public void editorCreated(EditorFactoryEvent event) {
        // nichts tun – BranchStateService setzt/cleart die Badge
    }

    @Override
    public void editorReleased(EditorFactoryEvent event) {
        Editor ed = event.getEditor();
        JComponent content = ed.getContentComponent();

        Object mark = content.getClientProperty(KEY_CORNER_BADGE);
        if (mark instanceof JComponent) {
            JComponent badge = (JComponent) mark;
            Container parent = badge.getParent();
            if (parent != null) {
                parent.remove(badge);
                parent.revalidate();
                parent.repaint();
            }
        }
        content.putClientProperty(KEY_CORNER_BADGE, null);
    }

    /** Rotes Badge-Widget (skalierbar) */
    static final class LiveBadgeCorner extends JComponent {
        private String text = "LIVE";
        private int scale = 1;

        LiveBadgeCorner(String text, int scale) {
            setOpaque(false);
            update(text, scale);
        }

        void update(String text, int scale) {
            this.text = (text == null || text.isBlank()) ? "LIVE" : text;
            this.scale = Math.max(1, scale);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int pad = 6 * scale;
            Font f = getFont().deriveFont(Font.BOLD, 12f * scale);
            FontMetrics fm = getFontMetrics(f);
            int w = fm.stringWidth(text) + pad * 2 + 4 * scale;
            int h = fm.getHeight() + pad * 2;
            return new Dimension(Math.max(64, w), Math.max(28, h));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int pad = 6 * scale;
                int radius = 12 * scale;

                g.setColor(new Color(255, 0, 0, 180));
                g.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

                g.setFont(getFont().deriveFont(Font.BOLD, 12f * scale));
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                int tx = pad;
                int ty = h - pad - fm.getDescent() + 1;
                g.drawString(text, tx, ty);
            } finally {
                g.dispose();
            }
        }
    }
}
