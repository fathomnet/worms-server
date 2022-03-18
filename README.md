# worms-server

Fast WoRMS name server for FathomNet use. Ingests a dump of the WoRMS database and serves out names and tree structures. The WoRMS data is simplified on load so that only the "Animalia" branch is used and all extinct species are removed.

## Endpoints

1. `/names` - returns all names ... there's a lot of names.
2. `/parent/:name` - return the name of the parent of `:name`. [Example](http://fathomnet.org:8888/parent/Bathochordaeus)
3. `/children/:name` - return the primary names of the children. [Example](http://fathomnet.org:8888/children/Bathochordaeus)
4. `/descendants/:name` - return the primary names of all the descendants [Example](http://fathomnet.org:8888/descendants/Atolla)
5. `/tree/:name` - return a structure tree from the provided name on down through the tree. [Example](http://fathomnet.org:8888/tree/Atolla)
6. `/query/startswith/:prefix` - returns all names that start with `:prefix`. [Example](http://fathomnet.org:8888/query/startswith/fish)
7. `/query/contains/:glob` - returns all the names that contain `:glob`. [Example](http://fathomnet.org:8888/query/contains/crab)
8. `/synonyms/:name` - returns alternative names for a term. [Example](http://fathomnet.org:8888/synonyms/Acanthonus%20armatus)

## Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `laikaSite` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.

## Libraries

- [circe](https://circe.github.io/circe/) for JSON handling
- [Methanol](https://github.com/mizosoft/methanol) with [Java's HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html) for HTTP client
- [munit](https://github.com/scalameta/munit) for testing
- [picocli](https://picocli.info/) for command line arg parsing
- [slf4j](http://www.slf4j.org/) with [logback](http://logback.qos.ch/) for logging. Use java.lang.System.Logger
- [ZIO](https://zio.dev/) for effects

## Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `laikaSite`.
