# TerraMath Mod for [Fabric/Forge] - 1.20/.1

![TerraMath_preview](https://github.com/user-attachments/assets/cfb099e5-1aa8-4940-9129-b8d0654072d0)


[![CurseForge](https://img.shields.io/curseforge/dt/1149108?style=for-the-badge&logo=curseforge&label=Curseforge&labelColor=black&color=red)](https://www.curseforge.com/minecraft/mc-mods/terra-math)
[![Modrinth](https://img.shields.io/modrinth/dt/terra-math?style=for-the-badge&logo=modrinth&label=Modrinth&labelColor=black&color=green)](https://modrinth.com/mod/terra-math)

[![Architecury API](https://img.shields.io/badge/Architectury%20API-REQUIRED-1?style=for-the-badge&labelColor=black&color=gold)](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-REQUIRED%20for%20Fabric-1?style=for-the-badge&labelColor=black&color=gold)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-blue?style=for-the-badge&labelColor=black)](https://www.minecraft.net)

## Description

TerraMath is a Minecraft 1.20.1 mod that allows you to customize world generation using mathematical functions. Create unique landscapes by defining terrain height through mathematical formulas!

## Features

- Custom world generation through mathematical formulas

- Adjustable basic generation parameters (scale, height, variation, smoothing)


## How to Use

<img width="594" alt="image" src="https://github.com/user-attachments/assets/13f44f0c-4e6e-45d4-8aff-506827f83f98">

- World generation settings interface

### Parameter Configuration

1. Go to "World" tab in world creation screen

2. Enter your desired formula in the field

3. Adjust additional parameters (optional):

   - Scale

   - Base Height (determines average surface level)

   - Height Variation (amplitude of changes)

   - Smoothing (terrain transition smoothness)

### Formula examples

- <details><summary>Basic wavy landscape: <code>sin(x)</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/9dc1c9c6-0b11-41d7-85cd-313291554d7d"></details>

- <details><summary>Spiky volcanic-like terrain with steep slopes: <code>abs(sin(x/10))*exp(cos(z/15))*8 + tanh(sqrt(x^2 + z^2)/20)*15</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/901f9f26-dc73-440f-903f-a728635db75e"></details>

- <details><summary>Rolling mountains: <code>sin(x/8)*cos(z/8)*10 + abs(sin(x/20))*15 + sqrt(abs(x/10))*5</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/8cc633e7-ae47-4df0-802c-5c8ee0d60a84"></details>

- <details><summary>Some crazy and heavy thing: <code>round(sin(x/15))*10 + round(cos(z/15))*10 + sqrt(abs(sin((x+z)/20)))*15</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/e29e4b4d-3f7b-46a0-ae7e-b31ed41fff85"></details>

### Available Functions and Operators

```
Mathematical Functions:

Trigonometric:
- sin(x) - sine
- cos(x) - cosine
- tan(x) - tangent
- asin(x) - inverse sine (arcsin)
- acos(x) - inverse cosine (arccos)
- atan(x) - inverse tangent (arctan)

Hyperbolic:
- sinh(x) - hyperbolic sine
- cosh(x) - hyperbolic cosine
- tanh(x) - hyperbolic tangent

Root and Power:
- sqrt(x) - square root
- cbrt(x) - cube root
- pow(x,y) - x raised to power y
- exp(x) - exponential (e^x)

Logarithmic:
- ln(x) - natural logarithm
- lg(x) - base-10 logarithm

Rounding and Numbers:
- abs(x) - absolute value
- floor(x) - largest integer less than x
- ceil(x) - smallest integer greater than x
- round(x) - rounds to nearest integer
- sign(x) - returns sign of x (-1, 0, or 1)
- mod(x,y) - remainder of x divided by y

Special Functions:
- gamma(x) - gamma function
- erf(x) - error function
- beta(x,y) - beta function

Utility Functions:
- max(x,y) - maximum of x and y
- min(x,y) - minimum of x and y
- sigmoid(x) - sigmoid function (1/(1+e^-x))
- clamp(x,min,max) - constrains x between min and max

Variables:
x, y, z - block coordinates in world

Operators:
+, -, *, /, ^, ()
```

## Known Issues

- Extreme formulas that produces XXL landscapes may impact performance (probably not a completely solvable problem)

## Future Plans
- Port to 1.20.2+

- Additional mathematical functions

- More terrain customization options

- Performance optimizations

- Better formula validation and error messages

## Contributing

Feel free to report bugs or suggest features through the issue tracker!
