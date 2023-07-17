plugins {
    java
    application
}

version = libs.versions.bcv.get()

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.quiltmc.org/repository/snapshot/")
}

dependencies {
    implementation(libs.bundles.asm)
    implementation(libs.bundles.bined)
    implementation(libs.bundles.commons)
    implementation(libs.bundles.darklaf)
    implementation(libs.bundles.jadx) {
        exclude(group = "org.smali", module = "smali")
    }
    implementation(libs.bundles.procyon)
    implementation(libs.bundles.smali)

    implementation(libs.antlr4) {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation(libs.apktool.lib) {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    implementation(libs.jadx.java.convert) {
        exclude(group = "com.android.tools", module = "r8")
        exclude(group = "com.jakewharton.android.repackaged", module = "dalvik-dx")
    }

    implementation(libs.quiltflower) {
        artifact {
            // "fat jar" containing QF plugins
            // this is required, otherwise gradle just downloads the slim jar which does not contain plugins
            // also it should be `all` but that build is currently broken
            classifier = ""
        }
    }

    implementation(libs.annotations)
    implementation(libs.apktool.cli)
    implementation(libs.cfr)
    implementation(libs.cloning)
    implementation(libs.dex2jar) {
        // These fail (for some reason?) so instead they're just loaded from the libs folder
        exclude("com.github.ThexXTURBOXx.dex2jar", "d2j-base-cmd")
        exclude("com.github.ThexXTURBOXx.dex2jar", "d2j-external")
    }
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.imgscalr)
    implementation(libs.janino)
    implementation(libs.jasm)
    implementation(libs.jgraphx)
    implementation(libs.js)
    implementation(libs.objenesis)
    implementation(libs.paged.data)
    implementation(libs.rsyntaxtextarea)
    implementation(libs.slf4j.api)
    implementation(libs.semver)
    implementation(libs.snakeyaml)
    implementation(libs.treelayout)
    implementation(libs.webp.imageio)
    implementation(libs.xpp3)

    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
}

application {
    mainClass.set("the.bytecode.club.bytecodeviewer.BytecodeViewer")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("**/module-info.class")
            filesMatching("META-INF/**") {
                if ("META-INF/services" !in path && "META-INF/plugins" !in path) {
                    exclude()
                }
            }
        }
        from(fileTree("dir" to "libs", "include" to listOf("*.zip")))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes(
                "Main-Class" to "the.bytecode.club.bytecodeviewer.BytecodeViewer",
                "Implementation-Title" to "Bytecode Viewer",
                "Implementation-Version" to libs.versions.bcv.get(),
                "Implementation-Vendor" to "The Bytecode Club",
                "X-Compile-Source-JDK" to "11",
                "X-Compile-Target-JDK" to "11",
            )
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
