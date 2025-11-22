pluginManagement {
    repositories {
        // 如果不是在 JitPack 服务器上，则使用阿里云镜像加速
        if (System.getenv("JITPACK") != "true") {
            maven("https://maven.aliyun.com/nexus/content/groups/public/")
            maven("https://maven.aliyun.com/nexus/content/repositories/google")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 如果不是在 JitPack 服务器上，则使用阿里云镜像加速
        if (System.getenv("JITPACK") != "true") {
            maven("https://maven.aliyun.com/nexus/content/groups/public/")
            maven("https://maven.aliyun.com/nexus/content/repositories/google")
        }
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "LzyEasyView"
include(":app")
include(":zymview")