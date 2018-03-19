# GLUE

## Description

GLUE (**G**enes **L**inked by **U**nderlying **E**volution) is a data-centric bioinformatics environment for virus sequence data, with a focus on variation, evolution and sequence interpretation.

You can learn more about it on the GLUE web site: [http://tools.glue.cvr.ac.uk](http://tools.glue.cvr.ac.uk)

## Build instructions

**NOTE: The vast majority of GLUE users probably do not need to build GLUE from source, and should install the binary distribution.**

You can find detailed instructions on how to install GLUE from its binary distribution here: [http://tools.glue.cvr.ac.uk/#/installation](http://tools.glue.cvr.ac.uk/#/installation).

Building from source is only required if you intend to fix bugs, develop new GLUE features, or understand in detail how GLUE works. 

In order to build GLUE you will need Java 1.8 or later and [https://gradle.org/](Gradle) 2.4. The build script may work with other Gradle versions but this has not been tested. You can download Gradle 2.4 here: [https://services.gradle.org/distributions/](https://services.gradle.org/distributions/).

Then you build the GLUE engine jar by running `gradle jar` in the `gluetools-core` directory: 

```
225pc177:gitrepos_ssh joshsinger$ cd gluetools/gluetools-core/
225pc177:gluetools-core joshsinger$ gradle jar
:gluetools-core:compileJava
:gluetools-core:processResources
:gluetools-core:classes
:gluetools-core:jar

BUILD SUCCESSFUL

Total time: 10.247 secs
```

Please contact the team if you are interested in building / running the web server version of GLUE.

## Contributing

The GLUE team is very open to new commands and modules contributed by the community. It is probably worth raising any ideas you have before embarking on development. Then we can discuss the design, and other aspects such as whether the required functionality belongs in the GLUE engine or if it can for example be introduced as project-specific scripting. 

Feel free to put forward any such ideas on the [GLUE support forum] (https://groups.google.com/forum/#!forum/glue-support).

## Credits

The GLUE software was written by Josh Singer. The copyright is held by the University of Glasgow.

## License

The project is licensed under the [GNU Affero General Public License v. 3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)