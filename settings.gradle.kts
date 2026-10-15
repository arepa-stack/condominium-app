pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS): the Kotlin/Wasm plugin registers its own
    // Node.js/Yarn/Binaryen repos per project; they are declared here instead.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        ivy("https://nodejs.org/dist") {
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            patternLayout { artifact("version_[revision]/[module]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "Condominio"
include(":androidApp", ":shared")
