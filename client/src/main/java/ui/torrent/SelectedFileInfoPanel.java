package ui.torrent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class SelectedFileInfoPanel {

    private final JPanel contentPane;

    public SelectedFileInfoPanel(File f) {
        contentPane = new JPanel(new GridBagLayout());

        if (f == null)
            return;

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.weightx = 1.0;
        labelGbc.fill = GridBagConstraints.HORIZONTAL;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = new Insets(4, 4, 4, 4);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.weightx = 2.0;
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.anchor = GridBagConstraints.NORTHWEST;
        valueGbc.insets = new Insets(4, 4, 4, 4);

        addProperty("Name", f.getName(), labelGbc, valueGbc);
        addProperty("Size", f.length() + " bytes", labelGbc, valueGbc);

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
