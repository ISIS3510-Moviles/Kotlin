pluginManagement {
    repositories {
        google {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CampusBites"
include(":app")
 