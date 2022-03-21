# worms-server

Fast [WoRMS](https://www.marinespecies.org) name server for FathomNet use. Ingests a dump of the WoRMS database and serves out names and tree structures. The WoRMS data is simplified on load so that only the "Animalia" branch is used and all extinct species are removed.

## Why?

WoRMS has its own [API](https://www.marinespecies.org/rest/) that is a fantastic resource if you're looking for information about a specific taxa. However, it has a few holes in its API that this project addresses. Most notably:

- WoRMS has no endpoint to get the names that are actually in WoRMS. To use that API, you have to already know what you are looking for. For web-sites and machine learning applications, we need to be able to ask ["What do you already know about?"](http://fathomnet.org:8888/names).
- WoRMS has methods to get the parent and children of a taxa, but lacks methods to get all the ancestors or descendants. This forces WoRMS API users to write their own recursive algorithms on top of the WoRMS API. This server provides simple methods to get a complete listing of all [ancestors](http://fathomnet.org:8888/ancestors/Atollidae) or [descendants](http://fathomnet.org:8888/descendants/Atollidae) of any taxa.
- WoRMS places a heavy emphasis on scientific names, making the barrier of entry to using common names, also called vernacular names,  high. For machine learning applications, we want to enable non-marine scientists to more easily ask for things like "[what are all the types of shrimp](http://fathomnet.org:8888/descendants/shrimps) or "[What squids are found in WoRMS](http://fathomnet.org:8888/query/contains/squid)"
- For the FathomNet website, we need the server responses to be fast. This server hold all data in memory for fast access and responses.

## Endpoints

- `/names` - returns all names ... there's a lot of names. The results are paged using query params `limit` (default 100) and `offset` (default 0). [Example](http://fathomnet.org:8888/names). [Example with limit and offset](http://fathomnet.org:8888/names?limit=500&offset=500000)
- `/names/count` - returns the total number of names available. [Example](http://fathomnet.org:8888/names/count)
- `/parent/:name` - return the name of the parent of `:name`. [Example](http://fathomnet.org:8888/parent/Bathochordaeus)
- `/children/:name` - return the primary names of the direct children. [Example](http://fathomnet.org:8888/children/Bathochordaeus)
- `/ancestors/:name` - return the primary names of all the ancestors in order from the top of the taxa tree down. [Example](http://fathomnet.org:8888/ancestors/Atolla)
- `/descendants/:name` - return the primary names of the taxa and all its descendants in alphabetical order. [Example](http://fathomnet.org:8888/descendants/Atolla)
- `/synonyms/:name` - returns alternative names for a term. The first term in the list is the primary/accepted name. [Example](http://fathomnet.org:8888/synonyms/Acanthonus%20armatus)
- `/query/startswith/:prefix` - returns all names that start with `:prefix`. [Example](http://fathomnet.org:8888/query/startswith/fish)
- `/query/contains/:glob` - returns all the names that contain `:glob`. [Example](http://fathomnet.org:8888/query/contains/crab)
- `/taxa/parent/:name` - returns the name, alternateNames, and rank of the parent of the term. [Example](http://fathomnet.org:8888/taxa/parent/Atolla)
- `/taxa/children/:name` - - returns the name, alternateNames, and rank of the children of the term. [Example](http://fathomnet.org:8888/taxa/children/Atolla)
- `/taxa/descendants/:name` - return a tree structure of descendants from the provided name on down through the tree. [Example](http://fathomnet.org:8888/taxa/descendants/Atollidae)
- `/taxa/info/:name` - returns the name, alternateNames, and rank of a term. [Example](http://fathomnet.org:8888/taxa/info/Atolla)

## Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `laikaSite` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.

## Libraries

- [circe](https://circe.github.io/circe/) for JSON handling
- [munit](https://github.com/scalameta/munit) for testing
- [picocli](https://picocli.info/) for command line arg parsing
- [slf4j](http://www.slf4j.org/) with [logback](http://logback.qos.ch/) for logging. Use [java.lang.System.Logger](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.Logger.html) with a [fluent decorator](src/main/scala/org/fathomnet/worms/etc/jdk/Logging.scala) in the code, it will act as a facade for slf4j.
- [ZIO](https://zio.dev/) for effects

## Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `laikaSite`.
