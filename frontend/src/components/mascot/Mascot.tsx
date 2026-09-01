import type { CSSProperties } from "react";

/**
 * Reagentic mascot - "Sage" the owl.
 *
 * Design rules (verified against the brand + Tarsi reference):
 *  - Single blue identity: body, head, outfit are all in the same blue family so
 *    the silhouette reads as one character, not "blue body + blue patch".
 *  - The t-shirt is a continuous panel that wraps the body. Hands and a held
 *    wallet emerge from it - no floating props.
 *  - Eyes are oversized (Tarsier DNA) with two highlights per eye for life.
 *  - Ambient motion is SVG-native (SMIL + CSS keyframes inside <defs>). No JS
 *    animation loops, no multiple SVGs, no GIFs.
 *  - prefers-reduced-motion is respected: every animation has a static fallback.
 */

type Tone = "light" | "dark";
type Mood = "idle" | "wave" | "celebrate";

type Props = {
  size?: number;
  tone?: Tone;
  mood?: Mood;
  className?: string;
  title?: string;
  style?: CSSProperties;
  /** Optional id used to scope <style> when multiple mascots appear on one page. */
  uid?: string;
};

const REAGENTIC_BLUE = "#2D43F5";
const REAGENTIC_CYAN = "#19C2F0";

export function Mascot({
  size = 220,
  tone = "light",
  mood = "idle",
  className,
  title = "Reagentic mascot",
  style,
  uid,
}: Props) {
  // Stable id so multiple mascots on one page do not collide on their <style>.
  const id = uid ?? "mascot";

  const outline = tone === "dark" ? "#0B0F2C" : "#1B2244";
  const bodyFill = tone === "dark" ? "#7A92E6" : "#6A86DF";
  const bodyShade = tone === "dark" ? "#4F66C2" : "#3F58C9";
  const belly = tone === "dark" ? "#F3F1EA" : "#FFFFFF";
  const shirt = REAGENTIC_BLUE; // t-shirt is the brand cobalt
  const hand = belly; // mitten hands read as a continuation of the shirt cuff
  const beak = tone === "dark" ? "#E8B341" : "#F1C04A";
  const feet = tone === "dark" ? "#E8B341" : "#F1C04A";
  const ground = tone === "dark" ? "rgba(255,255,255,0.06)" : "rgba(20,19,15,0.08)";

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 240 240"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label={title}
      className={`reagentic-mascot ${className ?? ""}`}
      style={style}
      data-mood={mood}
    >
      <title>{title}</title>
      <defs>
        <linearGradient id={`${id}-shirt`} x1="60" y1="130" x2="180" y2="220" gradientUnits="userSpaceOnUse">
          <stop stopColor={REAGENTIC_BLUE} />
          <stop offset="1" stopColor="#1F35D9" />
        </linearGradient>
        <linearGradient id={`${id}-coin`} x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#FFD86B" />
          <stop offset="1" stopColor="#E0A724" />
        </linearGradient>
        <radialGradient id={`${id}-ground`} cx="120" cy="220" r="70" gradientUnits="userSpaceOnUse">
          <stop stopColor={ground} />
          <stop offset="1" stopColor={ground.replace(/[\d.]+\)$/, "0)")} />
        </radialGradient>
        <linearGradient id={`${id}-cheek`} x1="0" y1="0" x2="0" y2="1">
          <stop stopColor="#F2A6B5" stopOpacity="0.55" />
          <stop offset="1" stopColor="#F2A6B5" stopOpacity="0" />
        </linearGradient>
        <clipPath id={`${id}-eye-l-clip`}><circle cx="96" cy="82" r="22" /></clipPath>
        <clipPath id={`${id}-eye-r-clip`}><circle cx="144" cy="82" r="22" /></clipPath>

        {/*
          All animation lives in CSS so the React tree is unchanged across moods.
          prefers-reduced-motion: reduce is honored - it removes the animation
          declaration and the character holds the static "rest" pose.
        */}
        <style>{`
          /* Scene breathes — origin at feet so shadow stays grounded */
          .${id}-scene { transform-origin: 120px 200px; transform-box: fill-box; animation: ${id}-breathe 4.2s ease-in-out infinite; }
          /* Head tilt — 6s, desynced from breathe */
          .${id}-head  { transform-origin: 120px  90px; transform-box: fill-box; animation: ${id}-tilt 6s ease-in-out infinite; }
          /* Eyelid: parked above disc when open (-48px), drops to cover disc when closed (28px). y=32 h=48 → open: -48 → y -16 (above 60), closed: 28 → y 60 (covers 60-108) */
          .${id}-lid-l, .${id}-lid-r { transform: translateY(-48px); animation: ${id}-blink 5.2s steps(1,end) infinite; }
          /* Right arm — idle rests, wave lifts */
          .${id}-right-arm { transform-origin: 86px 158px; transform-box: fill-box; }

          /* Mood overrides */
          [data-mood="celebrate"] .${id}-scene { animation: ${id}-hop .9s ease-in-out 1; }
          [data-mood="wave"] .${id}-right-arm { animation: ${id}-wave 1.15s cubic-bezier(0.34,1.56,0.64,1) infinite; }

          @keyframes ${id}-breathe { 0%,100% { transform: translateY(0) scale(1); } 50% { transform: translateY(-1.1px) scale(1.008); } }
          @keyframes ${id}-tilt    { 0%,100% { transform: rotate(-1deg); } 50% { transform: rotate(1deg); } }
          /* Blink — 95% open, 2% closed snap, no lid drift */
          @keyframes ${id}-blink   { 0%,92%,98%,100% { transform: translateY(-48px); } 94% { transform: translateY(28px); } 96% { transform: translateY(-48px); } }
          @keyframes ${id}-hop     { 0%,100% { transform: translateY(0); } 30% { transform: translateY(-6px); } 60% { transform: translateY(0); } }
          /* Wave — friendly & energetic: right hand (left holds wallet), big arc from shoulder with overshoot, 1.2s */
          @keyframes ${id}-wave    { 0% { transform: rotate(-74deg) scale(1); } 15% { transform: rotate(-96deg) scale(1.02); } 30% { transform: rotate(-58deg) scale(1); } 45% { transform: rotate(-92deg) scale(1.02); } 65% { transform: rotate(-62deg); } 85% { transform: rotate(-88deg); } 100% { transform: rotate(-74deg) scale(1); } }

          @media (prefers-reduced-motion: reduce) {
            .${id}-scene, .${id}-head, .${id}-lid-l, .${id}-lid-r, .${id}-coin, .${id}-left-arm, .${id}-right-arm {
              animation: none !important;
            }
          }
        `}</style>
      </defs>

      {/* Ground shadow - OUTSIDE the scene group so it does not float up with breathing */}
      <ellipse className={`${id}-ground-shadow`} cx="120" cy="222" rx="62" ry="6" fill={`url(#${id}-ground)`} />
      <g className={`${id}-scene`}>

        {/* Feet - simple gold boots so the character feels grounded */}
        <g stroke={outline} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M88 198 q8 -6 16 0 v12 q-8 6 -16 0 z" fill={feet} />
          <path d="M136 198 q8 -6 16 0 v12 q-8 6 -16 0 z" fill={feet} />
        </g>

        {/* Body - single teardrop */}
        <path
          d="M120 92 C 76 92 60 130 60 168 C 60 196 86 214 120 214 C 154 214 180 196 180 168 C 180 130 164 92 120 92 Z"
          fill={bodyFill}
          stroke={outline}
          strokeWidth="3"
          strokeLinejoin="round"
        />

        {/* T-shirt - a continuous panel that wraps the body */}
        <path
          d="M76 138 C 92 128 108 126 120 126 C 132 126 148 128 164 138 L 174 188 C 174 202 158 210 144 210 L 96 210 C 82 210 66 202 66 188 Z"
          fill={`url(#${id}-shirt)`}
          stroke={outline}
          strokeWidth="2.5"
          strokeLinejoin="round"
        />
        {/* Shirt collar V */}
        <path
          d="M104 130 L 120 146 L 136 130"
          stroke={outline}
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />

        {/* Reagentic double-chevron mark on the chest */}
        <g transform="translate(94 154) scale(0.78)">
          <path
            d="M12 9 L24 20 L12 31"
            stroke={belly}
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
            opacity="0.45"
          />
          <path
            d="M26 9 L40 20 L26 31"
            stroke={belly}
            strokeWidth="6.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </g>

        {/* Left arm (mascot's left = screen right) - holds the wallet */}
        <g className={`${id}-left-arm`}>
          <path
            d="M158 150 C 174 152 184 168 180 184 C 176 196 162 196 156 186 C 152 176 152 166 154 158 Z"
            fill={bodyShade}
            stroke={outline}
            strokeWidth="2.5"
            strokeLinejoin="round"
          />
          {/* Hand - small white mitten emerging from the sleeve cuff */}
          <circle cx="172" cy="186" r="9" fill={hand} stroke={outline} strokeWidth="2" />
          {/* Wallet - the prop the character is holding. Reagentic-blue card. */}
          <g className={`${id}-coin`} transform="translate(168 168)">
            <rect x="0" y="0" width="34" height="22" rx="4" fill={REAGENTIC_BLUE} stroke={outline} strokeWidth="2" />
            <rect x="4" y="4" width="26" height="3" rx="1.5" fill={belly} opacity="0.85" />
            <rect x="4" y="10" width="14" height="2" rx="1" fill={belly} opacity="0.55" />
            <circle cx="27" cy="16" r="2.4" fill={REAGENTIC_CYAN} />
          </g>
        </g>

        {/* Right arm - class drives the wave pose. In idle it rests; in wave it lifts up. */}
        <g className={`${id}-right-arm`}>
          <path
            d="M82 150 C 66 152 56 168 60 184 C 64 196 78 196 84 186 C 88 176 88 166 86 158 Z"
            fill={bodyShade}
            stroke={outline}
            strokeWidth="2.5"
            strokeLinejoin="round"
          />
          <circle cx="68" cy="186" r="9" fill={hand} stroke={outline} strokeWidth="2" />
        </g>

        {/* Head - drawn before ear tufts so the tufts sit on top */}
        <g className={`${id}-head`}>
          <path
            d="M120 30 C 84 30 64 56 64 86 C 64 116 88 138 120 138 C 152 138 176 116 176 86 C 176 56 156 30 120 30 Z"
            fill={bodyFill}
            stroke={outline}
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Ear tufts */}
          <path d="M70 84 L78 54 L92 78 Z" fill={bodyFill} stroke={outline} strokeWidth="2.5" strokeLinejoin="round" />
          <path d="M170 84 L162 54 L148 78 Z" fill={bodyFill} stroke={outline} strokeWidth="2.5" strokeLinejoin="round" />

          {/* Eye discs + irises + pupils + catchlights. Two groups so we can blink each independently. */}
          {/* Left eye - disc + clipped iris + animated lid (lid is the same color as the head so closing looks like a real eyelid drop) */}
          <circle cx="96" cy="82" r="22" fill={belly} stroke={outline} strokeWidth="2.5" />
          <g clipPath={`url(#${id}-eye-l-clip)`}>
            <circle cx="96" cy="84" r="12" fill={REAGENTIC_BLUE} />
            <circle cx="96" cy="84" r="5"  fill={outline} />
            <circle cx="100" cy="80" r="2.6" fill={belly} />
            <circle cx="92"  cy="89" r="1.4" fill={belly} opacity="0.7" />
            <rect className={`${id}-lid-l`} x="72" y="32" width="48" height="48" fill={bodyFill} />
          </g>
          <circle cx="144" cy="82" r="22" fill={belly} stroke={outline} strokeWidth="2.5" />
          <g clipPath={`url(#${id}-eye-r-clip)`}>
            <circle cx="144" cy="84" r="12" fill={REAGENTIC_BLUE} />
            <circle cx="144" cy="84" r="5"  fill={outline} />
            <circle cx="148" cy="80" r="2.6" fill={belly} />
            <circle cx="140" cy="89" r="1.4" fill={belly} opacity="0.7" />
            <rect className={`${id}-lid-r`} x="120" y="32" width="48" height="48" fill={bodyFill} />
          </g>

          {/* Beak */}
          <path d="M120 100 L114 112 L126 112 Z" fill={beak} stroke={outline} strokeWidth="2" strokeLinejoin="round" />
          {/* Smile - subtle curve under the beak, gives expression */}
          <path d="M112 118 q8 6 16 0" stroke={outline} strokeWidth="2" strokeLinecap="round" fill="none" />
          {/* Cheek blush */}
          <ellipse cx="78"  cy="108" rx="7" ry="3" fill={`url(#${id}-cheek)`} />
          <ellipse cx="162" cy="108" rx="7" ry="3" fill={`url(#${id}-cheek)`} />
          {/* Brow - gentle, gives the eyes weight */}
          <path d="M82 64 q14 -6 28 0" stroke={outline} strokeWidth="2.5" strokeLinecap="round" fill="none" />
          <path d="M130 64 q14 -6 28 0" stroke={outline} strokeWidth="2.5" strokeLinecap="round" fill="none" />

          
        </g>
      </g>
    </svg>
  );
}

export default Mascot;