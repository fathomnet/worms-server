# worms-server

A very fast [World Register of Marine Species (WoRMS)](https://www.marinespecies.org) name server for [FathomNet](http://fathomnet.org) use. Ingests a dump of the WoRMS database and serves out names and tree structures. The WoRMS data is simplified on load so that only the "Animalia" branch is used and all extinct species are removed.

## Why?

WoRMS has its own [API](https://www.marinespecies.org/rest/) that is a fantastic resource if you're looking for information about a specific taxa. However, FathomNet requires some features that are not available in the WoRMS API. Most notably:

- WoRMS lacks an endpoint to get the taxonomic names that are actually in WoRMS. To use that API, you have to already know what you are looking for. For web-sites and machine learning applications, we need to be able to ask ["What do you already know about?"](https://fathomnet.org/worms/names).
- WoRMS has methods to get the direct parent and children of a taxa, but lacks methods to get _all_ the ancestors or descendants. This forces WoRMS API users to write their own recursive algorithms on top of the WoRMS API. This server provides simple methods to get a complete listing of all [ancestors](https://fathomnet.org/worms/ancestors/Atollidae) or [descendants](https://fathomnet.org/worms/descendants/Atollidae) of any taxa. This feature is critical for aggregating machine learning datasets.
- Most WoRMS api endpoints require an _aphia id_ to use the endpoint. So at a minimum, every use of a WoRMS endpoint actually requires two HTTP requests: [one to look up the aphia id by a name](https://www.marinespecies.org/rest/AphiaIDByName/Atolla?marine_only=true) and then a second call to get the [piece of information you actually want](https://www.marinespecies.org/rest/AphiaVernacularsByAphiaID/135248). Our worms-server API is name-based, allowing naming requests to be fullfilled in a [single request](https://fathomnet.org/worms/taxa/info/Atolla).
- WoRMS places a heavy emphasis on scientific names, making the barrier of entry to using common names, also called vernacular names, high. For machine learning applications, we want to enable non-marine scientists to more easily ask for things like "[what are all the types of shrimp](https://fathomnet.org/worms/descendants/shrimps) or "[What squids are found in WoRMS](https://fathomnet.org/worms/query/contains/squid)"
- For the FathomNet website, we need the server responses to be fast. This server holds all data in memory for fast access and responses.

## Endpoints

### General information

- GET `/names` - returns all names ... there's a lot of names. The results are paged using query params `limit` (default 100) and `offset` (default 0). [Example](https://fathomnet.org/worms/names). [Example with limit and offset](https://fathomnet.org/worms/names?limit=500&offset=500000). Example response:

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

- GET `/names/count` - returns the total number of names available. [Example](https://fathomnet.org/worms/names/count). Response example: `872962`

- GET `/names/aphiaid/:aphiaid` - returns the names associated to a given aphiaid, a code that works assigns to every taxa. [Example](https://fathomnet.org/worms/names/aphiaid/125401). Example response:

```json
{
    "aphiaId": 125401,
    "name": "Swiftia pallida",
    "acceptedName": "Callistephanus pallida",
    "alternateNames": [
        "Northern sea fan coral",
        "Nördliche Seefeder",
        "corail pâle",
        "northern sea fan"
    ]
}
```

### Name requests

Unless otherwise indicated, these respond with a JSON array of Strings

- GET `/parent/:name` - return the name of the parent of `:name`. [Example](https://fathomnet.org/worms/parent/Bathochordaeus). Responds with a simple JSON string, e.g. `"Bathochordaeinae"`
- GET `/children/:name` - return the primary names of the direct children. [Example](https://fathomnet.org/worms/children/Bathochordaeus)
- GET `/ancestors/:name` - return the primary names of all the ancestors in order from the top of the taxa tree down. [Example](https://fathomnet.org/worms/ancestors/Atolla)
- GET `/descendants/:name` - return the primary names of the taxa and all its descendants in alphabetical order. [Example](https://fathomnet.org/worms/descendants/Atolla)
- GET `/synonyms/:name` - returns alternative names for a term. The first term in the list is the primary/accepted name. [Example](https://fathomnet.org/worms/synonyms/Acanthonus%20armatus)
- GET `/query/startswith/:prefix` - returns all names that start with `:prefix`. [Example](https://fathomnet.org/worms/query/startswith/fish)
- GET `/query/contains/:glob` - returns all the names that contain `:glob`. [Example](https://fathomnet.org/worms/query/contains/crab)

### Tree-structure requests

The following endpoints respond with a tree structure with each node like:

```json
{
  "name": "Biota",
  "rank": "",
  "aphiaId": 1,
  "acceptedAphiaId": 1,
  "alternateNames": [], 
  "children": []
}
```

`alternateNames` is an array of strings, `children` can be an array of nodes.

- GET `/taxa/parent/:name` - returns the name, alternateNames, and rank of the parent of the term. [Example](https://fathomnet.org/worms/taxa/parent/Atolla)
- GET `/taxa/children/:name` - returns the name, alternateNames, and rank of the children of the term. [Example](https://fathomnet.org/worms/taxa/children/Atolla)
- GET `/taxa/ancestors/:name` - return a tree structure from the root of the taxonomic tree down to the given name. Note that the last node will have it's children trimmed off. [Example](https://fathomnet.org/worms/taxa/ancestors/Atolla)
- GET `/taxa/descendants/:name` - return a tree structure of descendants from the provided name on down through the tree. [Example](https://fathomnet.org/worms/taxa/descendants/Atollidae)
- GET `/taxa/info/:name` - returns the name, alternateNames, and rank of a term. [Example](https://fathomnet.org/worms/taxa/info/Atolla)

## Developer Information

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

### Prerequisites

- [Java 17+](https://adoptium.net)
- [sbt](https://www.scala-sbt.org/)
- [Docker](https://www.docker.com/) - Optional

### Useful SBT Commands

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

#### Anywhere

The server can be run using:

```bash
docker run --name worms -d -p 8080:8080 -v "/local/path/to/worms/download/dir":"/opt/worms" mbari/worms-server
```

If you are an non-MBARI user and wish to run your own server, contact WoRMS for access to their database/text download. Once you have access, just download the worms zip file and extract it. You can easily run your own server with the above docker command. Your worms data dir, which contains the files `taxon.txt`, `vernacularname.txt`, and `speciesprofile.txt`, must be mounted into the container as `/opt/worms`.

#### MBARI

This repo contains a `build.sh` script that can build and stage the application to [MBARI's docker hub](https://hub.docker.com/repository/docker/mbari/worms-server). To run this application, download and extract the WoRMS download on eione.mbari.org. Eione has permissions from WoRMS to fetch their dataset.

In addition, we are merging in the `equipment` and `geological features` branches of the VARS Knowledgebase with the WoRMS tree. Internally, you can fetch the data in the correct format using:

```bash
docker run mbari/fathomnet-support kb-branch-to-worms-format \
  "http://dsg.mbari.org/kb/v1/phylogeny/down/equipment" 1000 > /path/to/worms/kb_equipment.csv

docker run mbari/fathomnet-support kb-branch-to-worms-format \
  "http://dsg.mbari.org/kb/v1/phylogeny/down/geological%20feature" 10000 > /path/to/worms/kb_geological_feature.csv
```

With those new CSV files in hand, the easiest way to include them is to drop them in the same directory as the worms files. Then launch the server like so:

```bash
docker run --name worms -d \
  -p 8080:8080 \
  -v "/local/path/to/worms/download/dir":"/opt/worms" \
  mbari/worms-server /opt/worms/kb_equipment.csv /opt/worms/kb_geological_feature.csv
```

The format of the CSV files is:

```csv
id,parentId,names
1000,,equipment
1001,1000,platform
1002,1000,Clathrate Bucket
1003,1000,Benthic Instrument Node;BIN
1004,1000,TPC;temperature, pressure, conductivity sensor
1005,1000,Wax corer
1006,1000,site marker
1007,1000,Dissolution Ball
1008,1001,Odor Pump
1009,1006,Remote Instrument Node;RIN
```

The values of the id aren't particularly important. On load they will be incremented so that they don't clash with any ids in worms or other trees. Once the trees are merged, all non-worms ids will be set to negative values to indicate they are not valid aphiaIds.

### Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `laikaSite`.
