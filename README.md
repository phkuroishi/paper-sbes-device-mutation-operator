# Device Mutation Operators

This repository contains all supplementary data and information regarding the paper entitled **"Designing Mutation Operators for Android Device Components:
A View Through Bluetooth and Location API’s"**, submitted to **XXXIX Simpósio Brasileiro de Engenharia de Software**.

Due to the conference's policies, the information about the authors was completely anonymized.

The repository is structured as follows:
```
paper-sbes-device-mutation-operator/
├── device-mutation-operator-data
└── cost-analysis
```
Next, a brief description of each directory is presented:

## `device-mutation-operator-data` directory
This directory contains all information used to define the set of mutation operators. It includes five files described as follows:

1. **BT_preliminary_identification_mutation_points.ods**: This file contains all identified mutation points. That is, the API/code structure related to Bluetooth that could be mutated.
2. **LOC_preliminary_identification_mutation_points.ods**: Similar to the previous file, it contains all candidate mutation points related to Location.
3. **DeviceMutationOperatorsDescrition.pdf**: This file contains an informal report of the 16 Android device mutation operators. It includes the *operator name*, *acronym*, *category*, *description*, and an *example* of each operator. 
4. **HAZOP.ods**: The file contains information regarding applying the HAZOP approach to the 16 mutation operators.
5. **literature_relationship.ods**: This file contains the data regarding the relationship between the existing mutation operators proposed in the academic literature and the mutation operators proposed in our work.

## `cost-analysis` directory
This directory contains all information regarding the cost analysis carried out in this work. It contains one directory and four different files as described below:

1. **app_info.ods**: This file contains information about the Android apps used in this analysis.
2. **app1_mutation_info.ods**: This file contains information about the generated mutants for APP1.
3. **app2_mutation_info.ods**: This file contains information about the generated mutants for APP2.
4. **statistics_per_operators.ods**: This file illustrates the statistics of the number of generated mutants per operator, considering APP1 and APP2.
5. **generated_mutants**: This directory contains all information regarding the mutants manually generated.