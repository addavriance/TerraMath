# TerraMath [Fabric/Forge] 1.20.x - 1.21.x

![TerraMath_preview](https://github.com/user-attachments/assets/cfb099e5-1aa8-4940-9129-b8d0654072d0)

[![CurseForge](https://img.shields.io/curseforge/dt/1149108?style=for-the-badge&logo=curseforge&label=Curseforge&labelColor=black&color=red)](https://www.curseforge.com/minecraft/mc-mods/terra-math)
[![Modrinth](https://img.shields.io/modrinth/dt/terra-math?style=for-the-badge&logo=modrinth&label=Modrinth&labelColor=black&color=green)](https://modrinth.com/mod/terra-math)

[![Fabric API](https://img.shields.io/badge/Fabric%20API-REQUIRED%20for%20Fabric-1?style=for-the-badge&labelColor=black&color=gold)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-blue?style=for-the-badge&labelColor=black)](https://www.minecraft.net)

Join the [Discord](https://discord.gg/JgKTZEtNkg) to share formulas or find new ones.

---

## Features

- Define terrain height with any math formula using `x`, `y`, `z` coordinates
- 50+ built-in functions: trig, hyperbolic, power, noise, random and more
- Configurable noise overlay (Perlin, Simplex, Blended, Normal)
- Adjustable generation parameters (scale, height, variation, smoothing)
- Real-time 3D formula preview in the settings screen
- Works on both client (world creation) and server (config file)

---

## Quick Start

1. Go to the **World** tab in the world creation screen
2. Click **Terrain Settings**
3. Enter a formula — for example: `sin(x) * cos(z) * 10`
4. Adjust scale, height and noise parameters as needed
5. Create the world

> **Full function reference**: [Wiki](https://github.com/addavriance/TerraMath/wiki/Functions-Reference)

### Formula Examples

- <details><summary>Basic wavy landscape: <code>sin(x)</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/9dc1c9c6-0b11-41d7-85cd-313291554d7d"></details>

- <details><summary>Rolling mountains: <code>sin(x/8)*cos(z/8)*10 + abs(sin(x/20))*15 + sqrt(abs(x/10))*5</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/8cc633e7-ae47-4df0-802c-5c8ee0d60a84"></details>

- <details><summary>Terraced landscape with plateaus: <code>floor(sin(x/20) * cos(z/20) * 5) * 4 + sqrt(x^2 + z^2)/10</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/7f6048d9-5ca4-4fb7-81d5-0a9d58ce22f3"></details>

- <details><summary>Spiky volcanic terrain: <code>abs(sin(x/10))*exp(cos(z/15))*8 + tanh(sqrt(x^2 + z^2)/20)*15</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/901f9f26-dc73-440f-903f-a728635db75e"></details>

- <details><summary>Crater-filled terrain: <code>10 * (1 - exp(-((x/30)^2 + (z/30)^2) / 2)) + 5 * perlin(x/50, 0, z/50)</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/a4e3d786-7dd9-4bd0-aed3-ba3e043b6b18"></details>

- <details><summary>Heavy stepped terrain: <code>round(sin(x/15))*10 + round(cos(z/15))*10 + sqrt(abs(sin((x+z)/20)))*15</code></summary><img width="594" alt="image" src="https://github.com/user-attachments/assets/e29e4b4d-3f7b-46a0-ae7e-b31ed41fff85"></details>

---

## Server Setup

1. Install the mod on the server
2. Edit `config/terramath.json`, or configure on a client and copy the file over
3. Set `"useDefaultFormula": true` for the config to apply to new worlds
4. Restart the server and generate a new world

---

## Known Issues

- Extreme formulas producing very large terrain features may impact performance
- Random functions (`rand`, `randnormal`, `randrange`) can cause chunk loading slowdowns — use sparingly

---

## Contributing

Bug reports and feature suggestions are welcome via the [issue tracker](../../issues).
