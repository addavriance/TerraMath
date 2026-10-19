# Changelog

## 1.4.0

### Added

- **Equation mode.** A formula with a single `=` (for example `y = 64 + 8 * sin(x / 10)`) is treated as an implicit surface: the world is solid where the left side is not greater than the right side. Formulas may now use `y`, which makes caves, floating islands and arbitrary 3D shapes possible
- **Live 3D preview** inside the settings screen. A rotatable voxel preview is rebuilt in the background while you type, and a notice is shown when there is not enough screen space for it
- Terrain settings are now available for all noise-based world types
- Builds for 1.21.4, 1.21.11, 26.1.2 and 26.2 on both Fabric and Forge. One codebase now produces 16 jars: 1.20, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.11, 26.1.2 and 26.2, each for Fabric and Forge

### Performance

- Compiled formulas are called through a typed interface
- Formula values are cached per column for formulas that do not use `y`, which speeds up chunk generation
- Noise lookups that do not depend on `y` (`simplex(x, z)`, `perlin(x, 0, z)`) are memoized per thread, so formulas that mix terrain noise with `y` no longer recompute it for every block
- The formula cache is thread-safe
- The preview evaluates in parallel and builds its mesh off the render thread

### Changed

- **Random formula generator** rewritten: it builds formulas from terrain archetypes, can crossbreed them, fits the scales to a sample and filters out dull results
- Dropped Architectury in favour of per-target Loom projects. Sources are split into `core`, `mc`, `loader` and `versions`
- Smoothing now applies only in Advanced mode, as in 1.3.0, and can be set to 0 to turn it off. Before, it was always on at 0.55 with no way to change it outside Advanced, which also made plain formulas five times more expensive
- Building now needs JDK 25 for the 26.x targets (Gradle downloads it automatically)

### Fixed

- The formula was applied only around spawn on some 1.20.1 worlds. It now covers every newly generated chunk and survives re-entering the world (#22)
- `floor` and `round` now return real numbers, so `1/floor(...)` gives infinity instead of failing with an integer division by zero, and `floor(5)/2` is `2.5`. Errors without a message no longer show up as `Syntax error: null` (#21)
- Points where a formula is not finite (`Infinity`, `NaN`) are treated as empty space instead of filling the world up to the build limit (#21)
- A formula that fails to compile no longer hangs a dedicated server: it is logged and the world generates empty instead of crashing on a client-only class

### Known issues

- Heavy formulas that use `y` together with several noise calls can slow chunk generation


## 1.3.0

## Overview
Version 1.3.0 introduces significant improvements to the TerraMath mod, including performance optimizations, new mathematical capabilities, enhanced error handling, and  server compatibility.

## What's New

### New Commands
- Added /terramath [info|formula|settings] command to display information about the world settings, current formula, or configuration settings.
- Added /formula command that outputs the current terrain generation formula in a copyable format for easy sharing.

### Mathematical Capabilities
Constants Support: Added support for mathematical constants
- Basic constants: pi (π), e, phi (φ)
- Advanced constants: zeta3 (ζ3), catalan (K), alpha (α), delta (δ), omega (Ω)
- All constants can be referenced by both symbol and name (e.g., π or pi)

### New Mathematical Functions
- atan2(x,y) - Two-argument inverse tangent
- root(x,n) - Calculate nth root of x
- gcd(x,y) - Greatest common divisor
- lcm(x,y) - Least common multiple
- modi(x,y) - Modular inverse
- and even more...

### Optimized Random Number Generation
- rand() - Generate random number between 0 and 1
- randnormal(mean,stdev) - Generate number from normal distribution
- randrange(min,max) - Generate random number in specified range

### Noise Functions
- perlin(x,y,z) - Perlin noise generator
- simplex(x,y,z) - Simplex noise generator
- normal(x,y,z) - Normal noise generator
- blended(x,y,z) - Blended noise (combines multiple types)
- octaved(x,z,octaves,persistence) - Octaved noise for more natural terrain

### Technical Improvements
- Formula Compilation: Formulas are now compiled into Java code using Janino rather than represented as node objects, significantly improving performance
- Error Handling: Added detailed syntax error handling with user-friendly notifications
- Dedicated Server Compatibility: Improved support for dedicated servers
    - Server configuration file handling
    - Fixed loading of noise settings from world data
    - Added notification system for corrupted formulas

### User Experience
- New config system
- Larger formula input field (up to 1024 characters)
- Added tooltip for long error messages
- Updated config translations for multiple languages
- Added modmenu description translation support

### Bug Fixes
- Fixed function shadowing when one name is a prefix of another
- Prevented null exceptions caused by integer division by zero
- Fixed checkbox state persistence
- Improved formula validation and error handling
- Fixed noise settings loading from world data
- Resolved several UI positioning issues

### Developer Changes
- Reorganized math package into specialized subpackages (parser, formula, functions)
- Reorganized utility functions into subpackages
- Replaced HashMap with ConcurrentHashMap to prevent concurrency issues
- Added terrain information command for debugging
- Code cleanup and performance improvements throughout

## Removed Features
- Removed dependency on Architectury API
- Removed redundant 1.5x multiplier from the base formula

## Coming Soon
- More formula examples in the community Discord
- Additional noise types and combinations
- Further performance optimizations for complex formulas


## 1.2.0

* Ported to versions 1.20.2-1.20.4


## 1.1.0

## New Features
* Added formula input field in World creation tab for custom terrain generation
* Implemented comprehensive mathematical function system including trigonometric, logarithmic and special functions
* Added support for multiple variables (x, y, z) in formulas

## Improvements
* Optimized formula parsing and evaluation system
* Added clear error messages for invalid formulas
* Enhanced formula validation to prevent incorrect inputs

## Bug Fixes
* Fixed incorrect height calculations in extreme cases
