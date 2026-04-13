# Design System Strategy: Frost-Glass Intelligence

## 1. Overview & Creative North Star
The "Frozen Wisdom" aesthetic is not merely a color palette; it is a clinical philosophy translated into interface design. Our Creative North Star is **"The Glacial Sanctuary."** This concept bridges the gap between cold, sterile medical data and the warm, human-centric nature of healthcare. 

By leveraging high-end editorial techniques—such as intentional white space, asymmetrical layouts, and glassmorphism—we move away from "dashboard fatigue" toward a high-fidelity experience that feels both authoritative and breathable. The system breaks the traditional rigid grid through the use of floating layers, overlapping frost-textures, and a hierarchy that relies on light and depth rather than lines and boxes.

---

## 2. Colors & Surface Philosophy
The palette is rooted in a "Light-Mode Frost" aesthetic, utilizing a sophisticated range of cyans and soft neutrals to create an environment of extreme clarity.

### The "No-Line" Rule
To achieve a premium, seamless feel, **1px solid borders are strictly prohibited for sectioning.** Boundaries must be defined through:
*   **Tonal Transitions:** Utilizing `surface-container-low` against a `surface` background.
*   **Soft Gradients:** Using linear transitions from `primary` to `primary-container` for active states.
*   **Shadow Depth:** Allowing the "stack" to define the edge.

### Surface Hierarchy & Nesting
Treat the UI as physical layers of frosted glass.
*   **Base Layer:** `surface` (#eff8f8) - The primary canvas.
*   **Floating Containers:** `surface-container-lowest` (#ffffff) - Used for the most important data cards to provide a "pop" of clean white.
*   **Sunken Elements:** `surface-container-high` (#d9e5e5) - For secondary information or sidebar backgrounds.

### The "Glass & Gradient" Rule
Floating elements (modals, tooltips, navigation) must utilize Glassmorphism.
*   **Recipe:** Use `primary-container` at 40% opacity + 20px `backdrop-blur`.
*   **Signature Finish:** Apply a subtle inner glow using a 1px white border at 20% opacity to mimic the edge of a frozen pane.

---

## 3. Typography
Our typography is a dialogue between the industrial precision of **Space Grotesk** and the humanist clarity of **Manrope**.

*   **Display & Headlines (Space Grotesk):** These are the "anchors." Use `display-lg` (3.5rem) with tighter letter-spacing (-0.02em) for a bold, editorial presence. This font conveys "Intelligence" and "Tech."
*   **Body & Titles (Manrope):** These are the "interpreters." Use `body-lg` (1rem) with a generous line-height (1.6) for readability. This font conveys "Care" and "Clinical Wisdom."
*   **Hierarchy:** Use dramatic scale shifts. A `display-md` headline should be paired with `label-md` metadata to create an authoritative, high-contrast visual rhythm.

---

## 4. Elevation & Depth
Depth in this system is achieved through **Tonal Layering**, mimicking how light passes through layers of ice.

*   **The Layering Principle:** Place `surface-container-lowest` cards on top of `surface-container-low` sections. This creates a natural "lift" without the visual clutter of drop shadows.
*   **Ambient Shadows:** When a true "float" is required (e.g., a floating action button), use a diffuse shadow: `box-shadow: 0 20px 40px rgba(0, 103, 96, 0.06)`. Note the tint—the shadow is a deep cyan (`primary-dim`), never pure black or grey.
*   **The Ghost Border Fallback:** If accessibility requires a border, use `outline-variant` at 15% opacity. It should be felt, not seen.

---

## 5. Components

### Buttons
*   **Primary:** A gradient fill from `primary` (#006760) to `primary-dim` (#005a54). Large `xl` (1.5rem) corner radius. No border.
*   **Secondary:** `surface-container-highest` background with `on-surface` text.
*   **Tertiary:** Transparent background with `primary` text. No box, only a slight scale-up on hover.

### Cards & Lists
*   **Strict Rule:** No divider lines. Separate items using `spacing-4` (1.4rem) of vertical white space.
*   **Styling:** Use `rounded-lg` (1rem) for standard cards. Apply a subtle "frost" hover state where the background shifts from `surface` to `surface-container-low`.

### Input Fields
*   **Style:** Minimalist. Underline-only or subtle "sunken" containers using `surface-container-low`.
*   **Active State:** Instead of a border change, use a `primary` glow (4px blur) and transition the label color to `primary`.

### Clinical Data Chips
*   **Interaction:** Use `selection-chips` with a Glassmorphism effect. When selected, the chip should gain a soft glow, as if lit from within, using `primary-container`.

---

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical layouts where text is offset from images to create an editorial feel.
*   **Do** use the `12` (4rem) and `16` (5.5rem) spacing tokens for section padding to allow the interface to "breathe."
*   **Do** use stroke-based icons with a consistent 1.5px weight, utilizing the `primary` color for emphasis.

### Don't
*   **Don't** use pure black (#000000) for text. Use `on-surface` (#283031) to maintain the soft, clinical tone.
*   **Don't** use standard 4px "Material Design" corners. This system requires `md` (0.75rem) or higher to maintain the "organic frost" feel.
*   **Don't** use high-contrast dividers. If a visual break is needed, use a background color shift or a horizontal gradient that fades to transparent.