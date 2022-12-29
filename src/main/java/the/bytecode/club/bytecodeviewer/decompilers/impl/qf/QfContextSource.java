package the.bytecode.club.bytecodeviewer.decompilers.impl.qf;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import the.bytecode.club.bytecodeviewer.BytecodeViewer;
import the.bytecode.club.bytecodeviewer.api.BCV;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QfContextSource implements IContextSource {
    private final Entries entries;
    private StringBuilder output = new StringBuilder();

    public QfContextSource(ClassNode cn) {
        List<Entry> entries = new ArrayList<>();
        Entry main = Entry.parse(cn.name);
        entries.add(main);
        if (cn.innerClasses != null) for (var inner : cn.innerClasses) {
            try {
                Objects.requireNonNull(BCV.getClassNode(inner.name)); // check if class is discoverable
                entries.add(Entry.parse(inner.name));
            } catch (Exception ignored) {
            }
        }

        this.entries = new Entries(entries, List.of(), List.of());
    }

    @Override
    public String getName() {
        return "BCV Quiltflower Context Source";
    }

    @Override
    public Entries getEntries() {
        return entries;
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
        ClassNode cn = BCV.getClassNode(resource);
        if (cn == null) {
            return null;
        }
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return new ByteArrayInputStream(cw.toByteArray());
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return new IOutputSink() {
            @Override
            public void begin() {
                output = new StringBuilder();
            }

            @Override
            public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
                output.append(content);
            }

            @Override
            public void acceptDirectory(String directory) {

            }

            @Override
            public void acceptOther(String path) {

            }

            @Override
            public void close() throws IOException {

            }
        };
    }

    public String getOutput() {
        return output.toString();
    }
}
