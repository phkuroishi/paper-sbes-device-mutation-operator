# Generated Mutants
This folder contains all information of the mutants manually generated for two applications considered in the study: *a2dpvolume* and *runnerup*.

The repository is divided into two subdirectories, as presented below:

```
cost-analysis/generated_mutants
├── mutants
└── source_code
```

# Subdirectories Description
## `mutants`directory
This folder contains all mutants manually generated for the two apps. Each directory contains the mutated file for the application. To check whether the application builds correctly, replace the original file from the app's directory with the mutated file and rebuild the project. 

To better visualize the generated mutants, just check their source code position in the **app1_mutation_info.ods** and see the associated mutated file.

## `source_code` directory
This directory contains the source code of each application.

# Building the mutants
Following the steps to build the mutants from each app:

## APP1: a2dpvolume
- Requires **Java 11**
- Build command: `./gradlew build`

## APP2: runnerup
- Requires **Java 17**
- Build command: `./gradlew build -x test`