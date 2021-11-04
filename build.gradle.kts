import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.5.31"
    id("org.openjfx.javafxplugin") version "0.0.10"
    id("org.jmailen.kotlinter") version "3.4.4"
    id("org.beryx.jlink") version "2.23.8"
}

group = "com.github.mikan.ipscan"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.neovisionaries:nv-oui:1.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "16"
        }
        dependsOn("genVersionFile")
    }

    application {
        mainModule.set("com.github.mikan.ipscan")
        mainClass.set("com.github.mikan.ipscan.MainKt")
    }

    javafx {
        version = "17.0.1"
        modules = listOf("javafx.controls", "javafx.fxml")
    }

    jlink {
        launcher {
            noConsole = true
        }
        forceMerge("kotlin")
        jpackage {
            when {
                Os.isFamily(Os.FAMILY_WINDOWS) -> {
                    imageOptions = listOf("--icon", "icon.ico")
                    installerOptions = listOf("--vendor", "mikan", "--win-shortcut")
                }
                Os.isFamily(Os.FAMILY_MAC) -> {
                    imageOptions = listOf("--icon", "icon.icns")
                    installerOptions = listOf("--vendor", "mikan")
                }
                else -> {
                    imageOptions = listOf("--icon", "icon.png")
                    installerOptions = listOf("--vendor", "mikan")
                }
            }
        }
    }

    register("genVersionFile") {
        doLast {
            val f = File(projectDir.absolutePath + "/src/main/resources/version.properties")
            f.createNewFile()
            f.writeText("app.version=$version\n")
        }
    }
}
