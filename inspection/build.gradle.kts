plugins {
  idea
  kotlin("jvm") version "1.3.31"
  id("org.jetbrains.intellij") version "0.4.8"
}

intellij {
  version = "2018.1"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
