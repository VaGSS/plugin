plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    // Spigot / Paper repos
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    // DiscordSRV repo
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    // Spigot API (works on Mohist 1.20.1 which implements Bukkit API)
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // DiscordSRV API (provided at runtime by installed DiscordSRV 1.30.0+)
    compileOnly("com.github.DiscordSRV:DiscordSRV:1.30.0")
}

tasks.jar {
    // do not shade; DiscordSRV is provided by the server
    archiveBaseName.set("DiscordWhitelistPlugin")
    archiveVersion.set("1.0.0")
}
