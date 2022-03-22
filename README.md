# worms-server

Fast [WoRMS](https://www.marinespecies.org) name server for FathomNet use. Ingests a dump of the WoRMS database and serves out names and tree structures. The WoRMS data is simplified on load so that only the "Animalia" branch is used and all extinct species are removed.

## Why?

WoRMS has its own [API](https://www.marinespecies.org/rest/) that is a fantastic resource if you're looking for information about a specific taxa. However, its API is missing features that this project addresses. Most notably:

- WoRMS has no endpoint to get the taxonomic names that are actually in WoRMS. To use that API, you have to already know what you are looking for. For web-sites and machine learning applications, we need to be able to ask ["What do you already know about?"](http://fathomnet.org:8888/names).
- WoRMS has methods to get the direct parent and children of a taxa, but lacks methods to get _all_ the ancestors or descendants. This forces WoRMS API users to write their own recursive algorithms on top of the WoRMS API. This server provides simple methods to get a complete listing of all [ancestors](http://fathomnet.org:8888/ancestors/Atollidae) or [descendants](http://fathomnet.org:8888/descendants/Atollidae) of any taxa. This feature is critical for aggregating machine learning datasets.
- Most WoRMS api endpoints require an _aphia id_ to use the endpoint. So at a minimum, every use of a WoRMS endpoint actually requires two HTTP requests: [one to look up the aphia id by a name](https://www.marinespecies.org/rest/AphiaIDByName/Atolla?marine_only=true) and then a second call to get the [piece of information you actually want](https://www.marinespecies.org/rest/AphiaVernacularsByAphiaID/135248). Our worms-server API is name-based, allowing naming requests to be fullfilled in a [single request](http://fathomnet.org:8888/taxa/info/Atolla).
- WoRMS places a heavy emphasis on scientific names, making the barrier of entry to using common names, also called vernacular names, high. For machine learning applications, we want to enable non-marine scientists to more easily ask for things like "[what are all the types of shrimp](http://fathomnet.org:8888/descendants/shrimps) or "[What squids are found in WoRMS](http://fathomnet.org:8888/query/contains/squid)"
- For the FathomNet website, we need the server responses to be fast. This server holds all data in memory for fast access and responses.

## Endpoints

### General information

- GET `/names` - returns all names ... there's a lot of names. The results are paged using query params `limit` (default 100) and `offset` (default 0). [Example](http://fathomnet.org:8888/names). [Example with limit and offset](http://fathomnet.org:8888/names?limit=500&offset=500000). Example response:

```json
{
  "items": [
  "  Breitflügelbussard",
  "  Doppelband-Regenpfeifer",
  "  Fuchslöffelente"
  ],
  "limit": 3,
  "offset": 0,
  "total": 872962
}
```

- GET `/names/count` - returns the total number of names available. [Example](http://fathomnet.org:8888/names/count). Response example: `872962`

### Name requests

Unless otherwise indicated, these respond with a JSON array of Strings

- GET `/parent/:name` - return the name of the parent of `:name`. [Example](http://fathomnet.org:8888/parent/Bathochordaeus). Responds with a simple JSON string, e.g. `"Bathochordaeinae"`
- GET `/children/:name` - return the primary names of the direct children. [Example](http://fathomnet.org:8888/children/Bathochordaeus)
- GET `/ancestors/:name` - return the primary names of all the ancestors in order from the top of the taxa tree down. [Example](http://fathomnet.org:8888/ancestors/Atolla)
- GET `/descendants/:name` - return the primary names of the taxa and all its descendants in alphabetical order. [Example](http://fathomnet.org:8888/descendants/Atolla)
- GET `/synonyms/:name` - returns alternative names for a term. The first term in the list is the primary/accepted name. [Example](http://fathomnet.org:8888/synonyms/Acanthonus%20armatus)
- GET `/query/startswith/:prefix` - returns all names that start with `:prefix`. [Example](http://fathomnet.org:8888/query/startswith/fish)
- GET `/query/contains/:glob` - returns all the names that contain `:glob`. [Example](http://fathomnet.org:8888/query/contains/crab)

### Tree-structure requests

The following endpoints respond with a tree structure with each node like:

```json
{
  "name": "Biota",
  "rank": "",
  "aphiaId": 1,
  "alternateNames": [], 
  "children": []
}
```

`alternateNames` is an array of strings, `children` can be an array of nodes.

- GET `/taxa/parent/:name` - returns the name, alternateNames, and rank of the parent of the term. [Example](http://fathomnet.org:8888/taxa/parent/Atolla)
- GET `/taxa/children/:name` - returns the name, alternateNames, and rank of the children of the term. [Example](http://fathomnet.org:8888/taxa/children/Atolla)
- GET `/taxa/ancestors/:name` - return a tree structure from the root of the taxonomic tree down to the given name. Note that the last node will have it's children trimmed off. [Example](http://fathomnet.org:8888/taxa/ancestors/Atolla)
- GET `/taxa/descendants/:name` - return a tree structure of descendants from the provided name on down through the tree. [Example](http://fathomnet.org:8888/taxa/descendants/Atollidae)
- GET `/taxa/info/:name` - returns the name, alternateNames, and rank of a term. [Example](http://fathomnet.org:8888/taxa/info/Atolla)

## Developer Information

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

### Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `laikaSite` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.

### Libraries

- [circe](https://circe.github.io/circe/) for JSON handling
- [munit](https://github.com/scalameta/munit) for testing
- [picocli](https://picocli.info/) for command line arg parsing
- [slf4j](http://www.slf4j.org/) with [logback](http://logback.qos.ch/) for logging. Use [java.lang.System.Logger](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.Logger.html) with a [fluent decorator](src/main/scala/org/fathomnet/worms/etc/jdk/Logging.scala) in the code, it will act as a facade for slf4j.
- [ZIO](https://zio.dev/) for effects

### Deployment

This repo contains a `build.sh` script that can build and stage the application to [MBARI's docker hub](https://hub.docker.com/repository/docker/mbari/worms-server). To run this application, download and extract the WoRMS download on eione.mbari.org. Eione has permissions from WoRMS to fetch their dataset. The server can be run using:

```bash
docker run --name worms -p 8080:8080 -v "/local/path/to/worms/download/dir":"/opt/worms" mbari/worms-server
```

If you are an non-MBARI user and wish to run your own server, contact WoRMS for access to their database/text download. Once you have access, just download the worms zip file and extract it. You can easily run your own server with the above docker command. Your worms data dir must be mounted into the container as `/opt/worms`.

### Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `laikaSite`.
