import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    implementation(group = "net.darkhax.curseforgegradle", name = "CurseForgeGradle", version = "1.1.26")
    implementation(group = "com.modrinth.minotaur", name = "Minotaur", version = "2.8.+")
}