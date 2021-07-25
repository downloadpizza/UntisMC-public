import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.60"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.60"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "UntisMC"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")
    compileOnly("org.spigotmc:spigot-api:1.14.4-R0.1-SNAPSHOT")
    compileOnly("org.bukkit:bukkit:1.14.4-R0.1-SNAPSHOT")

    implementation("com.github.kittinunf.fuel:fuel:2.2.1")
    implementation("com.beust:klaxon:5.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("src/main/kotlin")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.includeRuntime = true
    kotlinOptions.noStdlib = false
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        exclude("org/junit/**/*")
        exclude("org/intellij/**/*")
    }
}