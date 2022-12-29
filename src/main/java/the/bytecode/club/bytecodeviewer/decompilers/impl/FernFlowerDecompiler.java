package the.bytecode.club.bytecodeviewer.decompilers.impl;

import me.konloch.kontainer.io.DiskReader;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.objectweb.asm.tree.ClassNode;
import the.bytecode.club.bytecodeviewer.BytecodeViewer;
import the.bytecode.club.bytecodeviewer.api.ExceptionUI;
import the.bytecode.club.bytecodeviewer.decompilers.InternalDecompiler;
import the.bytecode.club.bytecodeviewer.decompilers.impl.qf.QfContextSource;
import the.bytecode.club.bytecodeviewer.translation.TranslatedStrings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private BaseDecompiler decompiler;

    private void constructDecompiler() {
        decompiler = new BaseDecompiler(
                new DirectoryResultSaver(new File(tempDirectory, "decompiled")),
                Map.of(),
                new PrintStreamLogger(System.out)
        );
    }

    @Override
    public void decompileToZip(String sourceJar, String zipName)
    {
        if (DecompilerContext.getCurrentContext() == null) {
            constructDecompiler();
        }
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
        if (DecompilerContext.getCurrentContext() == null) {
            constructDecompiler();
        }

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
}
