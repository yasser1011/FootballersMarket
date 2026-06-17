import React from "react";
import Switch from "@mui/material/Switch";
import wc26Logo from "./wc26.png";

// On/off switch (in a pill with the WC26 logo) that flips between normal mode
// ("/") and World Cup mode ("/worldcup"). The route is the source of truth.
const WorldCupToggle = ({ checked, onToggle }) => {
  return (
    <div
      className="wc-toggle"
      onClick={onToggle}
      role="switch"
      aria-checked={checked}
      aria-label="Toggle World Cup mode"
    >
      <img className="wc-toggle-logo" src={wc26Logo} alt="" />
      <span className="wc-toggle-label">World Cup</span>
      <Switch
        checked={checked}
        onChange={onToggle}
        size="small"
        onClick={(e) => e.stopPropagation()}
        sx={{
          "& .MuiSwitch-switchBase.Mui-checked": { color: "#e8b923" },
          "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
            backgroundColor: "#e8b923",
            opacity: 1,
          },
          "& .MuiSwitch-track": { backgroundColor: "rgba(255,255,255,0.45)" },
        }}
      />
    </div>
  );
};

export default WorldCupToggle;
