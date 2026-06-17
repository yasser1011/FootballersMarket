import React from "react";
import "./WorldCup.css";
import wcBannerLogo from "./wc26.png";

// Compact, centered World Cup header shown at the top of the /worldcup page.
const WorldCupHeader = () => {
  return (
    <div className="wc-head-row">
      <div className="wc-head">
        <img className="wc-head-logo" src={wcBannerLogo} alt="FIFA World Cup 2026" />
        <div className="wc-head-text">
          <div className="wc-head-title">FIFA World Cup 2026</div>
          <div className="wc-head-tagline">
            Predict World Cup match results. Scout top players. Earn points and
            compete with your friends.
          </div>
        </div>
      </div>
    </div>
  );
};

export default WorldCupHeader;
