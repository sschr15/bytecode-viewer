package the.bytecode.club.bytecodeviewer.compilers.impl;

import org.objectweb.asm.Opcodes;
import the.bytecode.club.bytecodeviewer.compilers.InternalCompiler;

import java.io.ByteArrayInputStream;

public class JasmAssembler extends InternalCompiler
{
    @Override
    public byte[] compile(String contents, String fullyQualifiedName) {
        var assembler = new com.roscopeco.jasm.JasmAssembler(
                fullyQualifiedName,
                () -> new ByteArrayInputStream(contents.getBytes())
        );
        return assembler.assemble();
    }
}
