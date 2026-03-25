import React from "react";
import "./style.css";

const FeaturedPlayer = ({ player, id }) => {
  return (
    <div className="top-players-row">
      <span>{id}</span>
      <div className="trending-player-imgs">
        <img
          src={player.playerImgUrl}
          alt=""
          className="trending-player-img"
          width={40}
          height={40}
        />
        <img
          src={player.playerTeamImgUrl}
          className="trending-player-club-img"
          width={21}
          height={21}
        ></img>
      </div>

      <div className="trending-player-data">
        <div className="trending-player-name-match">
          <span className="trending-player-name">{player.playerName}</span>
          <span className="trending-player-match">
            {player.homeTeamName} {player.homeTeamName === "" ? "" : "vs"}{" "}
            {player.awayTeamName}
          </span>
        </div>
        <div className="trending-player-match-score-container">
          <div className="trending-player-match-score-wrapper">
            <div className="trending-player-match-score-num">
              <span className="trending-player-match-score-num-span">
                <div className="trending-player-match-score-num-span-div">
                  <span>{player.rating}</span>
                </div>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeaturedPlayer;
