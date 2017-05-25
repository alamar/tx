name := """tx"""
organization := "ru.lj.alamar"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.11"

libraryDependencies += filters

libraryDependencies += "javax.mail" % "mail" % "1.4.7"
