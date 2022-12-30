package the.bytecode.club.bytecodeviewer.plugin.strategies;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.objectweb.asm.tree.ClassNode;
import the.bytecode.club.bytecodeviewer.api.Plugin;
import the.bytecode.club.bytecodeviewer.plugin.PluginLaunchStrategy;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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
 * @author Konloch
 * @author Bibl (don't ban me pls)
 * @since 06/25/2021
 */
public class JavascriptPluginLaunchStrategy implements PluginLaunchStrategy
{
    @Override
    public Plugin run(File file) throws Throwable
    {
        return new JsPlugin(Files.readString(file.toPath()), file.getName());
    }

    public static class JsPlugin extends Plugin {
        private final String script;
        private final String name;

        public JsPlugin(String script, String name) {
            this.script = script;
            this.name = name;
        }

        @Override
        public void execute(List<ClassNode> classNodeList) {
            try (Context ctx = Context.enter()) {
                Scriptable scope = ctx.initStandardObjects();
                ctx.evaluateString(scope, script, name, 1, null);
                Function function = (Function) scope.get("execute", scope);
                function.call(ctx, scope, scope, new Object[]{classNodeList});
            }
        }
    }
}
