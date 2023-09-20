# Table of Contents

- [General info](#general-info)
- [Contents of the Data Generation](#contents-of-the-data-generation)
- [Requirements](#requirements)

# General Info

This is the source code used by feTruth for data generation, including move method refactoring mining, method invocation retrieval, training data generation, and testing data generation.

# Contents of the Data Generation

/src/main/java:

- /refactoringminer: The source code for move method refactoring mining.

- /methodinvocation: The source code for method invocation retrieval.

- /datageneration: The source code for training and testing data generation.

/src/main/resources:

- /sql scripts: The scripts for database table creation.
- config.properties: The file for configuring the MySQL database and the project's root path.
- testingApps.txt: Testing projects.

# Requirements

- Java 11.0.17 or newer
- Apache Maven 3.8.1 or newer
