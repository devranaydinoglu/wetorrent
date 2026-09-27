package ui.torrent;

import torrent.FileInfo;
import torrent.MultiFileTorrentInfo;
import torrent.Torrent;
import torrent.TorrentInfo;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class SelectedFileInfoPanel {

    private final JPanel contentPane;

    public SelectedFileInfoPanel(Torrent torrent) {
        contentPane = new JPanel(new GridBagLayout());

        if (torrent == null)
            return;

        GridBagConstraints labelGbc = createLabelConstraints();
        GridBagConstraints valueGbc = createValueConstraints();

        TorrentInfo info = torrent.getTorrentInfo();
        addProperty("Name", info.getName(), labelGbc, valueGbc);
        addProperty("Size", formatSize(info.getLength()), labelGbc, valueGbc);

        if (info instanceof MultiFileTorrentInfo multiFileInfo) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (FileInfo fi : multiFileInfo.getFiles())
                model.addElement(String.join("/", fi.getPath()) + " (" + formatSize(fi.getLength()) + ")");

            JList<String> fileList = new JList<>(model);
            JScrollPane scrollPane = new JScrollPane(fileList);
            scrollPane.setPreferredSize(new Dimension(200, 150));

            addPropertyComponent("Files", scrollPane, labelGbc, valueGbc);
        }
    }

    public SelectedFileInfoPanel(File f) {
        contentPane = new JPanel(new GridBagLayout());

        if (f == null)
            return;

        GridBagConstraints labelGbc = createLabelConstraints();
        GridBagConstraints valueGbc = createValueConstraints();

        addProperty("Name", f.getName(), labelGbc, valueGbc);
        addProperty("Size", formatSize(sizeOf(f)), labelGbc, valueGbc);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String lastModified = formatter.format(Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()));
        addProperty("Last modified", lastModified, labelGbc, valueGbc);

        if (f.isDirectory()) {
            File[] children = f.listFiles();

            if (children == null) {
                addProperty("Contents", "Unable to read directory",
                    labelGbc, valueGbc);
            } else {
                Arrays.sort(children);

                DefaultListModel<String> model = new DefaultListModel<>();
                for (File child : children) {
                    if (!child.isHidden())
                        model.addElement(child.getName());
                }

                JList<String> fileList = new JList<>(model);
                JScrollPane scrollPane = new JScrollPane(fileList);
                scrollPane.setPreferredSize(new Dimension(200, 150));

                addPropertyComponent("Contents", scrollPane,
                    labelGbc, valueGbc);
            }
        }
    }

    private static long sizeOf(File f) {
        if (!f.isDirectory())
            return f.length();

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(f.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}

        return size.get();
    }

    private static String formatSize(long bytes) {
        return String.format("%.2f MiB", bytes / (1024.0 * 1024.0));
    }

    private static GridBagConstraints createLabelConstraints() {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.weightx = 1.0;
        labelGbc.fill = GridBagConstraints.HORIZONTAL;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = new Insets(4, 4, 4, 4);
        return labelGbc;
    }

    private static GridBagConstraints createValueConstraints() {
        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.weightx = 2.0;
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.anchor = GridBagConstraints.NORTHWEST;
        valueGbc.insets = new Insets(4, 4, 4, 4);
        return valueGbc;
    }

    private void addProperty(
        String name,
        String value,
        GridBagConstraints labelConstraints,
        GridBagConstraints valueConstraints) {

        addPropertyComponent(name, new JLabel(value),
            labelConstraints, valueConstraints);
    }

    private void addPropertyComponent(
        String name,
        Component value,
        GridBagConstraints labelConstraints,
        GridBagConstraints valueConstraints) {

        GridBagConstraints label = (GridBagConstraints) labelConstraints.clone();
        GridBagConstraints val = (GridBagConstraints) valueConstraints.clone();

        int row = contentPane.getComponentCount() / 2;

        label.gridy = row;
        val.gridy = row;

        contentPane.add(new JLabel(name + ":"), label);
        contentPane.add(value, val);
    }

    public JPanel getContentPane() {
        return contentPane;
    }
}
