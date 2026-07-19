# Rule-Based Decision Support System for Shipboard Firefighting (2008)

A legacy prototype of a rule-based decision support system developed in 2008 as a student project. The system was designed to assist operators in managing complex emergency situations by collecting available data, analyzing the operational picture, and presenting structured recommendations.

### Quick start

Just double-click `run.bat`. On the first run the application will automatically download a portable Java 8 (≈80 MB). Subsequent launches will start immediately.

<br/>
<p align="center">
    <img src="screenshot.png" width="800" />
</p>
<br/>

## Table of Contents

- [Project Overview](#project-overview)
- [Background and Motivation](#background-and-motivation)
- [Core Functionality](#core-functionality)
- [CLIPS Rule Engine](#clips-rule-engine)
- [Modernization (2026)](#modernization-2026)
- [How to Run](#how-to-run)
- [Technical Stack](#technical-stack)
- [Project Structure](#project-structure)
- [Limitations](#limitations)
- [Refactoring and Modernization Roadmap](#refactoring-and-modernization-roadmap)
- [License](#license)

## Project Overview

This project is a historical example of a rule-based expert system typical of the late 2000s. It integrates the CLIPS rule engine (originally developed by NASA) with domain-specific logic to support structured decision-making under time pressure. The deck plan used as a reference in the prototype corresponds to one of the decks of the Russian nuclear icebreaker *Arktika*, project 22220.

In the course of working on this student project, the author conducted a thorough review of the contemporary literature on intelligent systems and emerging directions in artificial intelligence. Alongside classical expert systems, significant attention was given to fuzzy logic, neural networks, machine learning, and evolutionary algorithms. The author also carried out practical experiments in training neural networks. Looking back two decades later, it is evident that these directions proved to be highly promising. Intelligent systems have since made remarkable progress, fundamentally transforming many fields of technology and industry.

## Background and Motivation

The prototype was created in response to the specific challenges of firefighting on large vessels, both civilian and naval, including nuclear-powered ships.

Large vessels represent complex engineering structures with dense equipment layouts, extensive forced ventilation systems, and high concentrations of combustible materials. Firefighting on such platforms is inherently difficult, even when supported by automated suppression systems. In specific, fire propagation is aggravated by the high thermal conductivity of metal bulkheads and decks, which can transfer heat to adjacent compartments even when direct flame contact is absent, requiring continuous cooling of bulkheads and decks. As a result, effective firefighting requires a high level of situational awareness, real-time information processing, coordination, and timely decision-making from the crew.

This prototype was developed to explore how an expert system could improve operator performance by automatically aggregating available sensor data and operational information, performing rule-based analysis, and delivering concise, actionable recommendations in a clear and concise form.

## Core Functionality

#### Expert System Operation:

- Initialization and loading of the CLIPS rule base at application startup
- Automatic analysis of the operational situation using the expert system rules
- Generation of a recommended actions table based on CLIPS inference results
- Support for multiple decision-making phases: evacuation, compartment sealing (making spaces gas-tight), prevention of ignition/explosion, fire containment, and firefighting
- Collection and processing of CLIPS data for selection of available fire hydrants, water supply positions, and fire containment boundaries
- Logging of the expert system inference process and results

#### Operational Situation Display:

- Deck compartment layout with indication of manned spaces and crew workstations
- Location and status of fire detectors, combustible and flammable materials, fire hydrants, and ventilation systems
- Interactive marking of equipment status, doors, and ventilation systems

#### Fire Prevention Recommendations:

- Evacuation routes for crew from compartments cut off by fire
- Compartment sealing (making spaces gas-tight), ventilation shutdown, and de-energizing of ship systems in threatened spaces
- Warnings of potential hazards associated with combustible and flammable materials

#### Fire Containment and Suppression Recommendations:

- Analysis of potential fire spread paths
- Rational selection of fire containment boundaries taking into account accessibility and crew safety
- Selection and positioning of available fire hoses for cooling bulkheads (walls)
- Sequence of offensive actions against seats of fire considering the compartment layout

## CLIPS Rule Engine

[CLIPS](https://www.clipsrules.net) (C Language Integrated Production System) serves as the core forward-chaining, data-driven rule engine of the system. In CLIPS, execution is controlled entirely by data: facts asserted into working memory (from sensors, user input, or previous inferences) automatically trigger pattern matching and rule activations. There is no explicit main control loop or direct calls to rules by name — the inference engine reactively manages the entire process.

This forward-chaining design brings several key advantages that align well with the demands of a real-time firefighting decision-support system on large vessels:

- **Reactivity and dynamism** — New or updated facts (e.g., sensor readings on fire spread, temperature, smoke, or compartment status) immediately cause re-evaluation of applicable rules. This makes the system naturally suited to monitoring evolving emergency situations, running “what-if” analyses, and generating timely recommendations without polling or explicit triggers.
- **Comprehensive inference** — The engine derives all possible conclusions from the current set of facts as long as rule activations exist. It does not stop at the first matching goal (as in goal-driven backward chaining) but continues propagating inferences, ensuring a rich operational picture and multiple layers of recommendations (evacuation, containment, suppression, etc.).
- **Declarative control through conflict resolution** — When multiple rules are activated simultaneously, the engine selects the next rule to fire according to salience values and configurable strategies (depth, breadth, LEX, MEA, etc.). Control remains declarative and focused on domain knowledge rather than procedural sequencing.
- **Efficient pattern matching at scale** — The built-in Rete algorithm enables fast matching even with large rule bases and frequent fact updates, which is essential in complex, sensor-rich maritime emergency scenarios.

Overall, the data-driven forward-chaining nature of CLIPS allows the expert system to continuously react to incoming information and produce structured, context-aware advice in highly dynamic and uncertain environments.

## Modernization (2026)

To enable execution on modern systems, the following changes were introduced:
* Migration to Gradle as the build automation tool
* Integration of a portable 32-bit Java 8 JRE due to the legacy 32-bit `CLIPSJNI.dll`. The project uses CLIPSJNI version 0.1 (the earliest official release). Since no official 64-bit CLIPSJNI binding was ever published, migration to a 64-bit JVM is currently blocked at the native integration layer.
* UTF-8 encoding configuration
* Minor adjustments for compatibility with current Windows environments
* Added support for English, German and Dutch languages (in addition to the original Russian) for the user interface and deck plans
* CLIPS rule engine output (inference logs and messages) remains in Russian only, as localization was not applied to the expert system console output

The original application logic, rule base, and user interface structure have been fully preserved.

## How to Run

### Quick Start (Recommended)

The simplest way to run the application is to use the provided batch script:

```bash
# Double-click run.bat or run from terminal
./run.bat
```

On the **first run**, the application will automatically download a portable 32-bit Java 8 JRE (~80 MB). This happens only once.

### Selecting Application Language

You can explicitly specify the interface language when launching:

```bash
./run.bat en     # English
./run.bat ru     # Russian
./run.bat de     # German
./run.bat nl     # Dutch
```

If no language is provided, the application uses the default language of your operating system.

### Alternative: Running via Gradle

For developers or CI environments, you can also run the application directly through Gradle:

```bash
# Run with default language (from OS)
./gradlew runApp

# Run with explicit language
./gradlew runApp -PappArgs=en
./gradlew runApp -PappArgs=de
```

### What Happens on First Launch

1. Gradle downloads a portable 32-bit JRE 8 into the `jre-8-32/` directory.
2. The `prepareRuntime` task copies `CLIPSJNI.dll` and the `clips/` directory (containing `feis.clp`) into the project root.
3. The application starts using the embedded JRE and loads the CLIPS rule base.

Subsequent launches are immediate and do not require an internet connection.

### Requirements

- Windows 10/11 (64-bit)
- Internet connection only on the very first run (for JRE download)
- No JDK/JRE installation required on the host machine

### Notes

- The application is designed to run with a 32-bit JVM because of the legacy `CLIPSJNI.dll` native library.
- All necessary native libraries and rule files are prepared automatically by Gradle tasks.
- The project maintains compatibility with the original 2008 runtime layout while using modern build tooling.

## Technical Stack

- **Rule Engine**: forward-chaining (via legacy CLIPSJNI 0.1 binding)
- **Native Integration**: 32-bit `CLIPSJNI.dll` (official early version from 2008)
- **User Interface**: Java Swing (original 2008 implementation)
- **Build System**: Gradle 8+
- **Java Compatibility**: Compiled for Java 8 (`-source 8 -target 8`), preserving the original Java 6-era coding style

## Project Structure

```
src/main/java/                  # Application source code
src/main/resources/
    └── clips                     # CLIPS rule base (core knowledge)
        └── feis.clp
    ├── map_en.png                # Deck plan (English)
    ├── map_ru.png                # Deck plan (Russian)
    ├── map_de.png                # Deck plan (German)
    ├── map_nl.png                # Deck plan (Dutch)
    └── i18n/                     # Localization resources
lib/
    ├── CLIPSJNI.dll              # Core CLIPS rule engine library
    └── CLIPSJNI.jar              # JNI wrapper for CLIPS
build.gradle
run.bat                         # Primary launcher for end users
jre-8-32/                       # Portable 32-bit JRE (downloaded automatically)
```

## Limitations

This is an educational prototype developed in the late 2000s. It is not intended for operational use.

The codebase reflects architectural patterns, coding practices, and engineering constraints typical of its time, including:
- Tight coupling between the user interface and domain logic
- Limited error handling and absence of automated tests
- Dependence on the earliest 32-bit CLIPSJNI binding (version 0.1), for which no official 64-bit version was ever released
- Design priorities focused on demonstrating rule-based reasoning rather than long-term maintainability
- CLIPS rule engine output (inference traces and messages printed by rules) is hardcoded in Russian; localization was not applied to the expert system console output

## Refactoring and Modernization Roadmap

The project is undergoing a structured modernization effort aimed at improving maintainability, testability, and long-term viability while preserving the original 2008 logic and behavior.

The following roadmap outlines the planned sequence of changes:

| Step | Action | Dependencies | Complexity | Goal |
|------|--------|--------------|------------|------|
| 1 | Introduce `ClipsEnvironment` interface with JNA-based implementation | — | Medium | Replace the legacy `CLIPSJNI` binding with a clean, controllable abstraction layer |
| 2 | Replace all direct usage of `CLIPSJNI.Environment` with `ClipsEnvironment` | 1 | Low | Isolate the project from the outdated JNI binding |
| 3 | Eliminate `goto` statements by extracting logic into dedicated methods | — | Medium | Improve code readability and remove a major source of technical debt |
| 4 | Generalize button positioning and visibility methods on the map | 3 | Medium | Reduce boilerplate code in the UI layer related to map elements |
| 5 | Introduce `ClipsInteractionService` | 2 | Medium | Create a dedicated orchestration layer between the UI and the CLIPS engine |
| 6 | Add automated tests for CLIPS interaction | 5 | Medium | Establish test coverage for the core decision-making logic |
| 7 | Migrate from 32-bit to 64-bit JVM | 1, 5 | High | Remove the legacy portable 32-bit JRE and simplify the runtime environment |
| 8 | Gradually introduce typed data models (DTOs) | 5 | Medium | Reduce reliance on raw string parsing from CLIPS responses |

## License

This project is provided for educational and historical purposes only.

## Acknowledgments

The author would like to express warm gratitude to Prof. Dr. Oleg V. Khrutsky for his thoughtful technical supervision of this project during its original development as a student work. The work was carried out at Saint Petersburg State Marine Technical University, Russia’s leading specialized institution for the education of engineers across the full range of marine technology—from shipbuilding and naval architecture to related technical and regulatory domains. The author is grateful for the rigorous engineering education received there, which provided the foundation for the author’s subsequent engineering path.
