import React from "react";
import "./CardsSection.css";
import FeaturedGame from "./FeaturedGame/FeaturedGame";
import FeaturedPlayers from "./FeaturedPlayers/FeaturedPlayers";
import UsersRanking from "./UsersRanking/UsersRanking";
import MySquad from "./MySquad/MySquad";
import WorldCupFixturesCard from "../worldcup/WorldCupFixturesCard";

const HomeHeaderCards = ({ worldCup = false }) => {
  return (
    <div className="cards-container-wrapper">
      <div className="cards-container">
        {/* world cup mode shows upcoming WC matches + predictions; normal mode keeps the
            existing featured (365) game card */}
        {worldCup ? <WorldCupFixturesCard /> : <FeaturedGame />}
        <FeaturedPlayers worldCup={worldCup} />
        <MySquad />
        <UsersRanking worldCup={worldCup} />
      </div>
    </div>
  );
};

export default HomeHeaderCards;
