plugins {
	kotlin("jvm") version "2.2.10"
	`maven-publish`
	java

	alias(libs.plugins.grgit)
	alias(libs.plugins.fabric.loom)
}

val archivesBaseName = "${project.property("archives_base_name").toString()}+mc${libs.versions.minecraft.get()}"
version = getModVersion()
group = project.property("maven_group")!!

repositories {
	maven("https://api.modrinth.com/maven")
	maven("https://maven.terraformersmc.com/")
	maven("https://maven.parchmentmc.org")
	maven("https://mvn.devos.one/snapshots")
	maven("https://maven.quiltmc.org/repository/release/")
}

//All dependencies and their versions are in ./gradle/libs.versions.toml
dependencies {

	minecraft(libs.minecraft)

	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-1.21.1:2024.11.17@zip")
	})

	//Fabric
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.api)

	//Mods
	modImplementation(libs.bundles.dependencies)
	modLocalRuntime(libs.bundles.dev.mods)

	include(modImplementation("gay.asoji:fmw:1.0.0+build.8")!!) // just to avoid the basic long metadata calls
}

// Write the version to the fabric.mod.json
tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mutableMapOf("version" to project.version))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName.get()}"}
	}
}

// This will attempt to publish the mod to the devOS Maven, otherwise it will build the mod locally
// This is auto run by GitHub Actions
task("buildOrPublish") {
	group = "build"
	var mavenUser = System.getenv().get("MAVEN_USER")
	if (!mavenUser.isNullOrEmpty()) {
		dependsOn(tasks.getByName("publish"))
		println("prepared for publish")
	} else {
		dependsOn(tasks.getByName("build"))
		println("prepared for build")
	}
}

// TODO: Uncomment for a non template mod!
publishing {
//	publications {
//		create<MavenPublication>("mavenJava") {
//			groupId = project.property("maven_group").toString()
//			artifactId = project.property("archives_base_name").toString()
//			version = getModVersion()
//
//			from(components.get("java"))
//		}
//	}
//
//	repositories {
//		maven {
//			url = uri("https://mvn.devos.one/${System.getenv()["PUBLISH_SUFFIX"]}/")
//			credentials {
//				username = System.getenv()["MAVEN_USER"]
//				password = System.getenv()["MAVEN_PASS"]
//			}
//		}
//	}
}

fun getModVersion(): String {
	val modVersion = project.property("mod_version")
	val buildId = System.getenv("GITHUB_RUN_NUMBER")

	// CI builds only
	if (buildId != null) {
		return "${modVersion}+build.${buildId}"
	}

	// If a git repo can't be found, grgit won't work, this non-null check exists so you don't run grgit stuff without a git repo
	if (grgit != null) {
		val head = grgit.head()
		var id = head.abbreviatedId

		// Flag the build if the build tree is not clean
		// (aka you have uncommitted changes)
		if (!grgit.status().isClean()) {
			id += "-dirty"
		}
		// ex: 1.0.0+rev.91949fa or 1.0.0+rev.91949fa-dirty
		return "${modVersion}+rev.${id}"
	}

	// No tracking information could be found about the build
	return "${modVersion}+unknown"

}