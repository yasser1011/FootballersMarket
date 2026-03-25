import React, { useContext, useEffect, useState } from "react";
import "./style.css";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import FeaturedPlayer from "./FeaturedPlayer";
import axios from "axios";
import { CircularProgress } from "@mui/material";
import { HomePageContext } from "../../Context/HomePageContext";
import { apiBaseUrl } from "../../config/Config";

const FeaturedPlayers = () => {
  const FETCH_STATUS = {
    IDLE: "idle",
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.IDLE);
  // const [featuredPlayers, setFeaturedPlayers] = useState([]);
  const { featuredPlayers, setFeaturedPlayers } = useContext(HomePageContext);

  let ranOnce = false;

  const fetchFeaturedPlayers = async () => {
    if (featuredPlayers != null) return;

    setFetchStatus(FETCH_STATUS.LOADING);
    try {
      const url = `${apiBaseUrl}/featured-players`;
      const featuredPlayersRes = await axios.get(url);
      if (featuredPlayersRes && featuredPlayersRes.data) {
        setFeaturedPlayers(featuredPlayersRes.data.featuredPlayers);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchFeaturedPlayers();
      ranOnce = true;
    }
  }, []);
  // let featuredPlayers = [
  //   {
  //     playerImg: "https://api.sofascore.app/api/v1/player/1090716/image",
  //     clubImg: "https://api.sofascore.app/api/v1/team/47504/image",
  //     playerName: "Cal Jennings",
  //     matchTeams: "Tampa Bay vs. Birmingham Legion",
  //     playerScore: "9.7",
  //   },
  //   {
  //     playerImg: "https://api.sofascore.app/api/v1/player/1090716/image",
  //     clubImg: "https://api.sofascore.app/api/v1/team/47504/image",
  //     playerName: "Cal Jennings",
  //     matchTeams: "Tampa Bay vs. Birmingham Legion",
  //     playerScore: "9.7",
  //   },
  //   {
  //     playerImg: "https://api.sofascore.app/api/v1/player/1090716/image",
  //     clubImg: "https://api.sofascore.app/api/v1/team/47504/image",
  //     playerName: "Cal Jennings",
  //     matchTeams: "Tampa Bay vs. Birmingham Legion",
  //     playerScore: "9.7",
  //   },
  //   {
  //     playerImg: "https://api.sofascore.app/api/v1/player/1090716/image",
  //     clubImg: "https://api.sofascore.app/api/v1/team/47504/image",
  //     playerName: "Cal Jennings",
  //     matchTeams: "Tampa Bay vs. Birmingham Legion",
  //     playerScore: "9.7",
  //   },
  //   {
  //     playerImg: "https://api.sofascore.app/api/v1/player/1090716/image",
  //     clubImg: "https://api.sofascore.app/api/v1/team/47504/image",
  //     playerName: "Cal Jennings",
  //     matchTeams: "Tampa Bay vs. Birmingham Legion",
  //     playerScore: "9.7",
  //   },
  // ];

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              height: "100%",
            }}
          >
            <CircularProgress />
          </div>
        </Card>
      </Box>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              height: "100%",
            }}
          >
            Error Please Try again Later
          </div>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ minWidth: 275 }}>
      <Card
        style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
        variant="outlined"
      >
        <div className="title-style">Featured Players</div>
        {featuredPlayers &&
          featuredPlayers.map((player, idx) => {
            return <FeaturedPlayer key={idx} player={player} id={idx + 1} />;
          })}
      </Card>
    </Box>
  );
};

export default FeaturedPlayers;
