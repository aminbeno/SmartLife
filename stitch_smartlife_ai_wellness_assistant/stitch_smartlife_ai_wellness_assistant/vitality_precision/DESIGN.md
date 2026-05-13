---
name: Vitality & Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3c4a46'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6b7a76'
  outline-variant: '#bacac5'
  surface-tint: '#006b5f'
  primary: '#006b5f'
  on-primary: '#ffffff'
  primary-container: '#2dd4bf'
  on-primary-container: '#00574d'
  inverse-primary: '#3cddc7'
  secondary: '#4648d4'
  on-secondary: '#ffffff'
  secondary-container: '#6063ee'
  on-secondary-container: '#fffbff'
  tertiary: '#855300'
  on-tertiary: '#ffffff'
  tertiary-container: '#ffad3a'
  on-tertiary-container: '#6d4400'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#62fae3'
  primary-fixed-dim: '#3cddc7'
  on-primary-fixed: '#00201c'
  on-primary-fixed-variant: '#005047'
  secondary-fixed: '#e1e0ff'
  secondary-fixed-dim: '#c0c1ff'
  on-secondary-fixed: '#07006c'
  on-secondary-fixed-variant: '#2f2ebe'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  baseline: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  margin-mobile: 16px
  gutter-mobile: 12px
---

## Brand & Style

The design system is rooted in the "Vitality & Precision" philosophy, specifically tailored for a native Android health experience. It utilizes **Material Design 3 (M3)** as its structural foundation, blending corporate reliability with an energetic, health-focused vibrance. 

The aesthetic is **Modern & Clean**, leaning into high-quality whitespace and purposeful motion to reduce cognitive load during health tracking. It avoids the clinical coldness of traditional medical apps by using soft Indigo accents and vibrant Mint primaries to evoke feelings of optimism, progress, and personal agency. The emotional response is one of "calm empowerment"—providing the user with clear, data-driven insights without inducing "data anxiety."

## Colors

The palette is designed for high legibility and functional signaling. 

*   **Primary (Vibrant Teal/Mint):** Used for key actions (Floating Action Buttons), active states, and progress indicators. It represents growth and vitality.
*   **Secondary (Soft Indigo):** Applied to supporting UI elements, category icons, and "wisdom" or "coaching" conversational elements to instill trust.
*   **Surface System:** Utilizes a "Clean White" base with "Subtle Gray" (#F8FAFC) for container backgrounds to create a clear distinction between the canvas and interactive cards.
*   **Semantic Colors:** Success (Leaf Green) and Warning (Amber) follow standard M3 logic but are tuned for high accessibility contrast against light surfaces.

## Typography

The design system employs **Inter** for all roles to ensure maximum readability and a systematic, tech-forward feel. 

Following the M3 scale, **Display** styles are reserved for high-level health metrics (e.g., daily step count). **Headlines** and **Titles** use a semi-bold weight (600) to provide strong visual anchors in data-heavy screens. **Body** text is optimized for readability with generous line heights, while **Labels** are used for micro-copy and chart annotations. On mobile devices, `display-lg` is rarely used; `headline-lg` serves as the primary page header.

## Layout & Spacing

The design system utilizes a **Fluid Grid** based on an 8dp (density-independent pixel) rhythm, standard for Android.

*   **Mobile:** 4-column layout with 16dp outer margins and 12dp gutters.
*   **Tablet:** 12-column layout with 24dp margins and 24dp gutters.
*   **Rhythm:** Vertical spacing between cards follows a 16dp (md) increment, while internal card padding is typically 16dp or 24dp to maintain a spacious, breathable feel. 
*   **Safe Areas:** Strict adherence to system bars and navigation bar heights is required, using tonal surface colors to bridge the gap between the app content and the system UI.

## Elevation & Depth

Depth is communicated through **Ambient Shadows** and **Tonal Layers**, strictly following Material 3's elevation system.

1.  **Level 0 (Flat):** The main background (#FFFFFF).
2.  **Level 1 (Tonal):** Surfaces for non-interactive content like background groupings, using the primary color at 5% opacity.
3.  **Level 2 (Raised):** Default state for interactive cards. Uses a soft, diffused shadow (Blur: 8dp, Y: 2dp, Opacity: 4% Black) to suggest lift without creating visual clutter.
4.  **Level 3 (Floating):** Reserved for Floating Action Buttons (FABs) and active dialogs, using a more pronounced shadow to indicate top-level hierarchy.

Backdrop blurs (Glassmorphism) are used sparingly, primarily for the top app bar during scroll states to maintain context of the content beneath.

## Shapes

The shape language is **Rounded**, reflecting the friendly and approachable nature of a wellness companion. 

*   **Cards & Containers:** Use `rounded-lg` (16dp) for standard health data cards. 
*   **Large Containers:** Bottom sheets and prominent dashboard sections use `rounded-xl` (24dp) on top corners.
*   **Buttons:** Standard buttons follow the M3 "Pill" shape (fully rounded) to maximize touch-target perception and visual softness.
*   **Conversational Bubbles:** AI or coaching messages use asymmetrical rounding (e.g., 16dp on three corners, 4dp on the anchor corner) to distinguish them from static UI cards.

## Components

The design system emphasizes clarity and ease of data entry.

*   **Buttons:** M3 Filled buttons for primary actions (e.g., "Start Workout"), Tonal buttons for secondary actions, and Outlined buttons for tertiary choices.
*   **Data Visualization:** Charts use the Primary Teal for the main data series and Secondary Indigo for comparisons. Line charts should be smoothed (spline) to match the rounded aesthetic.
*   **Conversational UI:** Chat bubbles are styled with light Indigo backgrounds for the "Coach" and Primary Teal for the "User." Avatars are circular and 40dp in diameter.
*   **Cards:** Every card includes a subtle 1dp border in a light gray (#E2E8F0) in addition to the shadow, ensuring definition even on bright screens.
*   **Chips:** Used for filtering health categories (e.g., Sleep, Nutrition, Steps). They feature an 8dp corner radius rather than the full pill-shape to distinguish them from action buttons.
*   **Input Fields:** M3 "Filled" style with a bottom-line indicator, but with top corners rounded to 8dp. Focus states use a 2dp Teal stroke.