plugins {
  idea
  kotlin("jvm") version "1.3.72"
  id("org.jetbrains.intellij") version "0.4.18"
}

intellij {
  version = "2019.3.4"
  setPlugins("java")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
