import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges
Laika / sourceDirectories     := Seq(baseDirectory.value / "docs")

ThisBuild / scalaVersion     := "3.5.1"
ThisBuild / organization     := "org.fathomnet"
ThisBuild / organizationName := "MBARI"
ThisBuild / startYear        := Some(2021)
ThisBuild / versionScheme    := Some("semver-spec")

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

Docker / maintainer := "Brian Schlining <brian@mbari.org>"

lazy val root = project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, GitBranchPrompt, GitVersioning, JavaAppPackaging, LaikaPlugin)
    .settings(
        name                      := "worms-server",
        // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
        // Use `show gitCurrentTags` in sbt to update/see the tags
        git.gitTagToVersionNumber := {
            tag: String =>
                if (tag matches "[0-9]+\\..*") Some(tag)
                else None
        },
        git.useGitDescribe        := true,
        // sbt-header
        dockerBaseImage           := "eclipse-temurin:21",
        dockerEntrypoint          := Seq("/opt/docker/bin/worms-server", "/opt/worms"),
        dockerExposedPorts        := Seq(8080),
        dockerExposedVolumes      := Seq("/opt/worms"),
        dockerRepository          := Some("mbari"),
        headerLicense             := Some(
            HeaderLicense.Custom(
                """Copyright (c) Monterey Bay Aquarium Research Institute 2022
        |
        |worms-server code is licensed under the MIT license.
        |""".stripMargin
            )
        ),
        javacOptions ++= Seq("-target", "17", "-source", "17"),
        laikaExtensions           := Seq(
            laika.markdown.github.GitHubFlavor,
            laika.parse.code.SyntaxHighlighting
        ),
        laikaIncludeAPI           := true,
        resolvers ++= Seq(
            Resolver.githubPackages("mbari-org", "maven")
        ),
        libraryDependencies ++= Seq(
            circeCore,
            circeGeneric,
            circeParser,
            jansi            % Runtime,
            logback          % Runtime,
            methanol,
            munit            % Test,
            picocli,
            slf4jJdk         % Runtime,
            tapirStubServer  % Test,
            tapirSwagger,
            tapirCirce,
            tapirCirceClient % Test,
            tapirNetty,
            tapirVertx,
            typesafeConfig,
            zio
        ),
        scalacOptions ++= Seq(
            "-deprecation", // Emit warning and location for usages of deprecated APIs.
            "-encoding",
            "UTF-8",        // yes, this is 2 args. Specify character encoding used by source files.
            "-feature",     // Emit warning and location for usages of features that should be imported explicitly.
            "-language:existentials",
            "-language:higherKinds",
            "-language:implicitConversions",
            "-language:postfixOps",
            "-unchecked",
            "-Wunused:imports"
        )
    )

// https://stackoverflow.com/questions/22772812/using-sbt-native-packager-how-can-i-simply-prepend-a-directory-to-my-bash-scrip
bashScriptExtraDefines ++= Seq(
    """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
    """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""
)
batScriptExtraDefines ++= Seq(
    """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf"""",
    """call :add_java "-Dlogback.configurationFile=%APP_HOME%\conf\logback.xml""""
)
