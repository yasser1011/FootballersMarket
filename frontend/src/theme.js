import { createTheme } from "@mui/material/styles";

// The primary color's single source of truth is the CSS variable --app-primary
// (defined in index.css). We read it here so MUI components (buttons, etc.)
// match the navbar/header CSS without duplicating the value. MUI needs a real
// color string (it computes hover/contrast shades), so a var() can't be passed
// directly — hence the read. The fallback covers the rare case the stylesheet
// isn't applied yet at startup.
const primary =
  getComputedStyle(document.documentElement)
    .getPropertyValue("--app-primary")
    .trim() || "#232f3e"; /* alt: #2c3ec4 */

// App-wide light theme. Primary = the navy/blue used by the navbar and table
// headers, so all MUI primary buttons read as that color instead of MUI blue.
export const appTheme = createTheme({
  palette: {
    primary: { main: primary, contrastText: "#ffffff" },
  },
});
