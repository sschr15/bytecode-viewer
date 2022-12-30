package the.bytecode.club.bytecodeviewer.decompilers.impl;

import com.android.tools.r8.internal.JL;
import me.konloch.kontainer.io.DiskReader;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.objectweb.asm.tree.ClassNode;
import the.bytecode.club.bytecodeviewer.BytecodeViewer;
import the.bytecode.club.bytecodeviewer.api.ExceptionUI;
import the.bytecode.club.bytecodeviewer.decompilers.InternalDecompiler;
import the.bytecode.club.bytecodeviewer.decompilers.impl.qf.QfContextSource;
import the.bytecode.club.bytecodeviewer.translation.TranslatedStrings;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static the.bytecode.club.bytecodeviewer.Constants.nl;
import static the.bytecode.club.bytecodeviewer.Constants.tempDirectory;
import static the.bytecode.club.bytecodeviewer.translation.TranslatedStrings.ERROR;
import static the.bytecode.club.bytecodeviewer.translation.TranslatedStrings.FERNFLOWER;

/***************************************************************************
 * Bytecode Viewer (BCV) - Java & Android Reverse Engineering Suite        *
 * Copyright (C) 2014 Kalen 'Konloch' Kinloch - http://bytecodeviewer.com  *
 *                                                                         *
 * This program is free software: you can redistribute it and/or modify    *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation, either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 ***************************************************************************/

/**
 * A FernFlower wrapper with all the options (except 2)
 *
 * @author Konloch
 * @author WaterWolf
 * @since 09/26/2011
 */
public class FernFlowerDecompiler extends InternalDecompiler
{
    private final Map<String, JComponent> OPTIONS = new HashMap<>();
    private BaseDecompiler decompiler;

    private void constructDecompiler() {
        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<String, JComponent> entry : OPTIONS.entrySet()) {
            if (entry.getKey().equals(IFernflowerPreferences.USER_RENAMER_CLASS) && ((JTextField) entry.getValue()).getText().isBlank()) {
                continue;
            }
            if (entry.getValue() instanceof JCheckBox) {
                options.put(entry.getKey(), ((JCheckBox) entry.getValue()).isSelected() ? "1" : "0");
            } else if (entry.getValue() instanceof JTextField) {
                options.put(entry.getKey(), ((JTextField) entry.getValue()).getText());
            } else if (entry.getValue() instanceof JSpinner) {
                options.put(entry.getKey(), ((JSpinner) entry.getValue()).getValue().toString());
            }
        }

        decompiler = new BaseDecompiler(
                new DirectoryResultSaver(new File(tempDirectory, "decompiled")),
                options,
                new PrintStreamLogger(System.out)
        );
    }

    @Override
    public void decompileToZip(String sourceJar, String zipName)
    {
        constructDecompiler();
        decompiler.addSource(new File(sourceJar));
        decompiler.decompileContext();

        Path decompiledPath = Path.of(tempDirectory, "decompiled");
        Path zipPath = Path.of(zipName);

        try (FileSystem zipFs = FileSystems.newFileSystem(zipPath.toUri(), Map.of())) {
            Files.copy(decompiledPath, zipFs.getPath("/"));
        } catch (IOException e) {
            BytecodeViewer.handleException(e);
        }
    }

    @Override
    public String decompileClassNode(final ClassNode cn, byte[] b)
    {
        constructDecompiler();

        Exception exception;
        try {
            QfContextSource contextSource = new QfContextSource(cn);
            decompiler.addSource(contextSource);
            decompiler.decompileContext();

            return contextSource.getOutput();
        } catch (Exception e) {
            exception = e;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        return FERNFLOWER + " " + ERROR + "! " + ExceptionUI.SEND_STACKTRACE_TO +
                nl + nl + TranslatedStrings.SUGGESTED_FIX_DECOMPILER_ERROR +
                nl + nl + sw;
    }

    public void setupSettingsMenu(JMenu settings) {
        OPTIONS.clear();
        Map<String, Object> defaults = IFernflowerPreferences.DEFAULTS;

        for (Field f : IFernflowerPreferences.class.getFields()) {
            IFernflowerPreferences.Name name = f.getAnnotation(IFernflowerPreferences.Name.class);
            IFernflowerPreferences.Description description = f.getAnnotation(IFernflowerPreferences.Description.class);

            if (name == null || description == null) {
                continue;
            }

            String internalName;
            try {
                internalName = (String) f.get(null);
            } catch (Exception e) {
                BytecodeViewer.handleException(e);
                continue;
            }

            String value = (String) defaults.get(internalName);
            JComponent component;
            if (value == null) {
                JLabel label = new JLabel(name.value());
                label.setHorizontalAlignment(SwingConstants.LEFT);
                JTextField textField = new JTextField();
                textField.setToolTipText(description.value());
                component = textField;
                settings.add(label);
            } else if (value.equals("0")) {
                JCheckBox checkBox = new JCheckBox(name.value());
                checkBox.setToolTipText(description.value());
                checkBox.setSelected(false);
                component = checkBox;
            } else if (value.equals("1")) {
                JCheckBox checkBox = new JCheckBox(name.value());
                checkBox.setToolTipText(description.value());
                checkBox.setSelected(true);
                component = checkBox;
            } else if (value.matches("\\d+")) {
                JLabel label = new JLabel(name.value());
                label.setHorizontalAlignment(SwingConstants.LEFT);
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt(value), 0, Integer.MAX_VALUE, 1));
                spinner.setToolTipText(description.value());
                component = spinner;
                settings.add(label);
            } else {
                JLabel label = new JLabel(name.value());
                label.setHorizontalAlignment(SwingConstants.LEFT);
                JTextField textField = new JTextField(value);
                textField.setToolTipText(description.value());
                component = textField;
                settings.add(label);
            }
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(390, 25));
            settings.add(component);
            OPTIONS.put(internalName, component);
        }
    }

    public void setDefaults() {
        Map<String, Object> defaults = IFernflowerPreferences.DEFAULTS;
        for (Map.Entry<String, JComponent> entry : OPTIONS.entrySet()) {
            if (entry.getValue() instanceof JCheckBox) {
                ((JCheckBox) entry.getValue()).setSelected(defaults.get(entry.getKey()).equals("1"));
            } else if (entry.getValue() instanceof JTextField) {
                ((JTextField) entry.getValue()).setText((String) defaults.get(entry.getKey()));
            } else if (entry.getValue() instanceof JSpinner) {
                ((JSpinner) entry.getValue()).setValue(Integer.parseInt((String) defaults.get(entry.getKey())));
            }
        }
    }

    public String outputConfig() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JComponent> entry : OPTIONS.entrySet()) {
            String value;
            if (entry.getValue() instanceof JCheckBox) {
                value = ((JCheckBox) entry.getValue()).isSelected() ? "1" : "0";
            } else if (entry.getValue() instanceof JTextField) {
                value = ((JTextField) entry.getValue()).getText();
            } else if (entry.getValue() instanceof JSpinner) {
                value = ((JSpinner) entry.getValue()).getValue().toString();
            } else {
                continue;
            }
            if (!value.equals(IFernflowerPreferences.DEFAULTS.get(entry.getKey()))) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(entry.getKey()).append("=").append(value);
            }
        }
        return sb.toString();
    }

    public void inputConfig(String options) {
        for (String option : options.split(" ")) {
            String[] split = option.split("=");
            if (split.length != 2) {
                continue;
            }
            Component component = OPTIONS.get(split[0]);
            if (component == null) {
                continue;
            }
            if (component instanceof JCheckBox) {
                ((JCheckBox) component).setSelected(split[1].equals("1"));
            } else if (component instanceof JTextField) {
                ((JTextField) component).setText(split[1]);
            } else if (component instanceof JSpinner) {
                ((JSpinner) component).setValue(Integer.parseInt(split[1]));
            }
        }
    }

    public void inputOldConfig(Map<String, Boolean> options) {
        for (Map.Entry<String, Boolean> entry : options.entrySet()) {
            Component component = OPTIONS.get(entry.getKey());
            if (component == null || entry.getValue() == null) {
                continue;
            }
            if (component instanceof JCheckBox) {
                ((JCheckBox) component).setSelected(entry.getValue());
            }
        }
    }
}
