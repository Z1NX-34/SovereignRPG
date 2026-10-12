# Sovereign Class UI

Client-side skin for **Class Selection Mod 1.6.0** (Forge 1.20.1). Re-skins the class, race and side
screens with textured gold-and-violet panels, buttons and corner ornaments, replaces the animated
smoke/snow background (cheaper on weak GPUs), and pulls content inward so it sits inside the frame.
It contains no code or assets from the Class Selection Mod; it hooks into it at runtime.
Class Selection Mod belongs to its authors ("ClassMod Team", All Rights Reserved) - credit them.

## Build (no local setup): GitHub Actions
1. Create a new GitHub repo and upload the contents of this folder.
2. Open the repo's **Actions** tab, run **build** (or just push). Download the `sovereignclassui-jar` artifact.
3. Put the jar in `mods/` next to `classmod-1.6.0.jar`.

Local build: needs JDK 17 and **Gradle 8.x** (ForgeGradle 6 does not work with Gradle 9): `gradle build`,
jar in `build/libs/`.

## Override textures from your texture pack
Ship files with the same paths in your pack under `assets/sovereignclassui/textures/gui/`:

| file | size | notes |
|---|---|---|
| panel.png | 32x32 | 9-slice, border 8 |
| card.png | 32x32 | 9-slice, border 6 (class cards, header banner) |
| mini.png | 16x16 | 9-slice, border 4 |
| button_normal / button_hover / button_disabled.png | 24x24 | 9-slice, border 6 |
| corner_tl / corner_tr / corner_bl / corner_br.png | 40x40 | frame corner ornaments |

Keep the sizes (or change the numbers in `Skin.java`).

## Tuning
`LayoutMixin` - how far content is pulled inward. `Skin.background` - frame, vignette, margins.
`GuiThemeMixin` - which draw call maps to which texture.

## Known limits
Written against class mod 1.6.0 only. If the class mod updates and renames methods the game will
crash on load with a mixin error (deliberate, so it is not silent). Not compiled or run by the author
of this file - first build may need small fixes.


## Update 2 (race cards, bigger class panel)

* **Race screen** is now big square cards side by side. Put a picture for each race at
  `assets/sovereignclassui/textures/gui/race/<race id>.png` (square, 256x256 is ideal; any size works,
  it is stretched to the square). Your texture pack can supply them. When a race has no picture, a default
  emblem is shown and the panel tells you the exact file name it is looking for.
  Bundled placeholders: `human`, `humans`, `god`, `gods`, `_default`.
* **Class screen**: details panel no longer overlaps the cards, is taller, and the attribute section is a
  3-column grid that scrolls with the mouse wheel (so many attributes fit). Starter kit is listed under it.
* Hint / counter texts are brighter. Frame padding reduced.
