# GLUE

<img src="md/glue-logo.png" align="right" alt="" width="280" />

Welcome to the GitHub repository for the **GLUE** software framework!

GLUE (**G**enes **L**inked by **U**nderlying **E**volution) is an open, integrated software toolkit, implemented in Java™, that provides functionality for storage, management, and analysis of sequence data 

GLUE provides a framework for the development of sequence data-oriented ‘projects’ that focus on a specific genome features, or small genomes (e.g. viral genomes), and contain the data items required for performing comparative genomics investigations (e.g., sequences, genome feature annotations, alignments, and phylogenies, and any other relevant data).

## Key Features of GLUE:

-   **Evolutionary Data Organization**: Virus nucleotide and protein sequences are structured along evolutionary lines, facilitating comparative genomics and systematic studies of viral diversity.

-   **Customizable Bioinformatics Workflows**: GLUE supports integration with standard bioinformatics tools such as BLAST, RAxML, and MAFFT, and users can create custom analysis workflows tailored to specific research needs, from simple analyses to complex pipelines.

-   **Flexible Data Schema**: The underlying data schema can be extended to represent additional biological, epidemiological, or clinical data. GLUE allows custom annotations, metadata integration, and comprehensive data queries through its powerful command layer.

-   **Reference-Constrained Alignments**: GLUE offers robust handling of reference-constrained alignments, capturing coding features at the nucleotide and amino acid levels, providing high precision for evolutionary and structural analyses.

-   **Seamless Containerization & Web Services**: Projects can be rapidly developed offline and published as part of web services or microservices architectures, benefiting from GLUE's containerization support for ease of deployment and reproducibility.

-   **Collaborative Project Management**: GLUE's tight integration with GitHub enables decentralized project development, allowing teams to work independently and merge data or analysis workflows when necessary.

-   **Integrated Phylogenetics & Sequence Analysis**: Phylogenetic tree generation, sequence variation analyses, and metadata-driven querying provide powerful tools for evolutionary and epidemiological investigations, supported by standardized and custom datasets.

For more information, please visit the GLUE web site: [http://tools.glue.cvr.ac.uk](http://tools.glue.cvr.ac.uk)

## Build instructions

**NOTE: The vast majority of GLUE users probably do not need to build GLUE from source, and should install the binary distribution.**

You can find detailed instructions on how to install GLUE from its binary distribution here: [http://tools.glue.cvr.ac.uk/#/installation](http://tools.glue.cvr.ac.uk/#/installation).

Building from source is only required if you intend to fix bugs, develop new GLUE features, or understand in detail how GLUE works. 

In order to build GLUE you will need the [Java 1.8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Gradle](https://gradle.org/) 4.10.2. The build script may work with other Gradle versions but this has not been tested. You can download Gradle 4.10.2 here: [https://services.gradle.org/distributions/](https://services.gradle.org/distributions/).

Build the GLUE engine jar by running `gradle jar` in the `gluetools-core` directory: 

```
$ cd gluetools/gluetools-core/
$ gradle jar
:gluetools-core:compileJava
:gluetools-core:processResources
:gluetools-core:classes
:gluetools-core:jar

BUILD SUCCESSFUL

Total time: 10.247 secs
```

This will create a new jar file in `gluetools-core/build/libs`.

Please contact the team if you are interested in building / running the web server version of GLUE.

## Development environment

We suggest importing the `gluetools-core` project into Eclipse 4.4 or later for development.

## Contact

For questions, issues, or feedback, please contact us at [gluetools@gmail.com](mailto:gluetools@gmail.com) or open an issue on the [GitHub repository](https://github.com/giffordlabcvr/gluetools/issues).

## Contributing

The GLUE team is very open to new commands and modules contributed by the community. It is probably worth raising any ideas you have with the team before embarking on development. Then we can discuss the design, and other aspects such as whether the required functionality belongs in the GLUE engine or if it can for example be introduced as project-specific scripting. 

Feel free to put forward any such ideas on the [GLUE support forum](https://groups.google.com/forum/#!forum/glue-support).

If contributing to GLUE, please review our [Contribution Guidelines](./md/CONTRIBUTING.md).

[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](./md/code_of_conduct.md) 

## Credits

The GLUE software was written by Josh Singer. The copyright is held by the University of Glasgow.

## License

The project is licensed under the [GNU Affero General Public License v. 3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
