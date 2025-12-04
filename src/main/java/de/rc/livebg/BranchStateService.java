package de.rc.branchwarner;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service(Service.Level.PROJECT)
public final class BranchStateService implements AutoCloseable {

    private final Project project;
    private final Set<VirtualFile> headFiles = new HashSet<>();
    private volatile String currentBranch = null;
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> { var t = new Thread(r, "BranchWarnerPoll"); t.setDaemon(true); return t; });
    private final ScheduledFuture<?> future;

    public BranchStateService(Project project) {
        this.project = project;

        collectHeadFiles();
        recompute();

        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent ev : events) {
                    var vf = ev.getFile();
                    var p  = ev.getPath();
                    if ((vf != null && headFiles.contains(vf)) || (p != null && p.endsWith("/.git/HEAD"))) {
                        recompute();
                        return;
                    }
                }
            }
        });

        future = exec.scheduleAtFixedRate(this::recompute, 1, 1, TimeUnit.SECONDS);
    }

    // ---------- Git / Branch ----------

    private void collectHeadFiles() {
        headFiles.clear();
        var lfs = LocalFileSystem.getInstance();

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
            File head = new File(root.getPath(), ".git/HEAD");
            if (head.isFile()) {
                var vf = lfs.refreshAndFindFileByIoFile(head);
                if (vf != null) headFiles.add(vf);
            }
        }
        String base = project.getBasePath();
        if (base != null) {
            File head = new File(base, ".git/HEAD");
            if (head.isFile()) {
                var vf = lfs.refreshAndFindFileByIoFile(head);
                if (vf != null) headFiles.add(vf);
            }
        }
    }

    private static String readHeadBranch(VirtualFile head) {
        try {
            String text = new String(head.contentsToByteArray(), StandardCharsets.UTF_8).trim();
            if (text.startsWith("ref:")) {
                String ref = text.substring(text.indexOf(':') + 1).trim(); // refs/heads/BRANCH
                int idx = ref.lastIndexOf('/');
                return (idx >= 0) ? ref.substring(idx + 1) : ref;
            }
            return null; // detached
        } catch (IOException e) { return null; }
    }

    private String resolveBranchViaGitApi() {
        try {
            var mgr = GitRepositoryManager.getInstance(project);
            for (GitRepository repo : mgr.getRepositories()) {
                var name = repo.getCurrentBranchName();
                if (name != null && !name.isBlank()) return name;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isWarnBranch(String branch, LiveBadgeSettings.State state) {
        if (branch == null || state == null || state.branches == null) return false;
        for (String t : state.branches.split(",")) {
            if (t.trim().equalsIgnoreCase(branch)) return true;
        }
        return false;
    }


    private static void positionBadge(JLayeredPane layered,
                                  JComponent badge,
                                  JViewport viewport,
                                  String corner) {

		Rectangle vr = SwingUtilities.convertRectangle(
				viewport.getParent(),
				viewport.getBounds(),
				layered
		);

		Dimension ps = badge.getPreferredSize();
		int margin = 8;

		int x;
		int y;

		if ("TOP_LEFT".equalsIgnoreCase(corner)) {
			x = vr.x + margin;
			y = vr.y + margin;
		} else if ("TOP_RIGHT".equalsIgnoreCase(corner)) {
			x = vr.x + Math.max(0, vr.width - ps.width - margin);
			y = vr.y + margin;
		} else if ("BOTTOM_LEFT".equalsIgnoreCase(corner)) {
			x = vr.x + margin;
			y = vr.y + Math.max(0, vr.height - ps.height - margin);
		} else {
			// Default: BOTTOM_RIGHT
			x = vr.x + Math.max(0, vr.width - ps.width - margin);
			y = vr.y + Math.max(0, vr.height - ps.height - margin);
		}

		badge.setBounds(x, y, ps.width, ps.height);
	}



	private static void installRelayoutHooks(JComponent content,
											 JLayeredPane layered,
											 JComponent badge,
											 JViewport viewport) {
		final String KEY_HOOKS = "rc.live.hooksInstalled";
		if (Boolean.TRUE.equals(content.getClientProperty(KEY_HOOKS))) return;

		ComponentAdapter relayout = new ComponentAdapter() {
			private void relayoutNow() {
				if (!badge.isShowing()) return;
				Object c = badge.getClientProperty("rc.live.corner");
				String corner = (c instanceof String) ? (String) c : "BOTTOM_RIGHT";
				positionBadge(layered, badge, viewport, corner);
				layered.repaint(badge.getBounds());
			}

			@Override public void componentResized(ComponentEvent e) { relayoutNow(); }
			@Override public void componentMoved(ComponentEvent e)   { relayoutNow(); }
			@Override public void componentShown(ComponentEvent e)   { relayoutNow(); }
			@Override public void componentHidden(ComponentEvent e)  { relayoutNow(); }
		};

		viewport.addComponentListener(relayout);
		layered.addComponentListener(relayout);
		content.putClientProperty(KEY_HOOKS, Boolean.TRUE);
	}


    private void setBadge(Editor ed, String branch, int scale, String corner) {
        JComponent content = ed.getContentComponent();

        // Viewport bestimmen
        JScrollPane sp = (ed instanceof EditorEx) ? ((EditorEx) ed).getScrollPane()
                : (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, content);
        if (sp == null) return;
        JViewport viewport = sp.getViewport();
        if (viewport == null) return;

        // Root-Layer holen
        JRootPane root = SwingUtilities.getRootPane(content);
        if (root == null) return;
        JLayeredPane layered = root.getLayeredPane();
        if (layered == null) return;

        // Badge-Komponente verwalten
        Object existing = content.getClientProperty(EditorOverlayInstaller.KEY_CORNER_BADGE);
        EditorOverlayInstaller.LiveBadgeCorner badge;
        String text = (branch != null ? branch.toUpperCase() : "LIVE");

        if (existing instanceof EditorOverlayInstaller.LiveBadgeCorner) {
            badge = (EditorOverlayInstaller.LiveBadgeCorner) existing;
            badge.update(text, scale);
        } else {
            badge = new EditorOverlayInstaller.LiveBadgeCorner(text, scale);
            content.putClientProperty(EditorOverlayInstaller.KEY_CORNER_BADGE, badge);
            layered.add(badge, JLayeredPane.DRAG_LAYER);
        }

		badge.putClientProperty("rc.live.corner", corner);

		positionBadge(layered, badge, viewport, corner);
		layered.setComponentZOrder(badge, 0);
		badge.setVisible(true);
		layered.revalidate();
		layered.repaint(badge.getBounds());

		installRelayoutHooks(content, layered, badge, viewport);

		//System.out.println("[BranchWarner] setBadge-overlay: branch=" + text
		//		+ " scale=" + scale + " corner=" + corner);
    }

    private void clearBadge(Editor ed) {
        JComponent content = ed.getContentComponent();
        Object mark = content.getClientProperty(EditorOverlayInstaller.KEY_CORNER_BADGE);
        if (mark instanceof Component) {
            Component c = (Component) mark;
            Container parent = c.getParent();
            if (parent instanceof JLayeredPane) {
                ((JLayeredPane) parent).remove(c);
                parent.revalidate();
                parent.repaint();
            } else if (parent != null) {
                parent.remove(c);
                parent.revalidate();
                parent.repaint();
            }
        }
        content.putClientProperty(EditorOverlayInstaller.KEY_CORNER_BADGE, null);
    }

    // ---------- Recompute ----------

        // ---------- Recompute ----------

	private void recompute() {
		if (headFiles.isEmpty()) collectHeadFiles();

		String branch = resolveBranchViaGitApi();
		if (branch == null) {
			for (VirtualFile head : headFiles) {
				String b = readHeadBranch(head);
				if (b != null) { branch = b; break; }
			}
		}

		// Einstellungen laden (unverändert, nur gleich danach Corner ziehen)
		LiveBadgeSettings cfg = project.getService(LiveBadgeSettings.class);
		LiveBadgeSettings.State st =
				(cfg != null) ? cfg.getStateOrDefault() : new LiveBadgeSettings.State();

		final boolean anyWarn      = isWarnBranch(branch, st);
		final String  displayText  = (branch != null ? branch.toUpperCase() : "UNKNOWN");
		final int     displayScale = Math.max(1, st.scale);

		// NEU: Corner aus dem State lesen, mit Default
		final String corner =
				(st.corner != null && !st.corner.isBlank())
						? st.corner
						: "BOTTOM_RIGHT";

		// Optionales Logging, wenn du magst:
		// System.out.println("[BranchWarner] recompute: branch=" + displayText
		//        + " warn=" + anyWarn + " scale=" + displayScale + " corner=" + corner);

		Editor[] editors = EditorFactory.getInstance().getAllEditors();
		Editor selected  = FileEditorManager.getInstance(project).getSelectedTextEditor();

		for (Editor ed : editors) {
			if (ed.getProject() != this.project) {
				continue;
			}

			// Nur im aktiven Editor anzeigen – in allen anderen aufräumen
			if (!anyWarn || ed != selected) {
				clearBadge(ed);
			} else {
				// NEU: corner mitgeben
				setBadge(ed, displayText, displayScale, corner);
			}
		}
	}



    @Override public void close() {
        headFiles.clear();
        future.cancel(true);
        exec.shutdownNow();
    }
}
