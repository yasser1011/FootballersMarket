import React from "react";
import "./CardsSection.css";
import FeaturedGame from "./FeaturedGame/FeaturedGame";
import FeaturedPlayers from "./FeaturedPlayers/FeaturedPlayers";
import UsersRanking from "./UsersRanking/UsersRanking";

const HomeHeaderCards = () => {
  return (
    <div className="cards-container-wrapper">
      <div className="cards-container">
        <FeaturedGame />
        <FeaturedPlayers />
        <UsersRanking />
      </div>
    </div>
  );
};

export default HomeHeaderCards;
