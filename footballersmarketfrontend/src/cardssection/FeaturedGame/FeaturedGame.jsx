import React, { useContext, useEffect, useState } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import "./FeaturedGame.css";
import { CircularProgress } from "@mui/material";
import axios from "axios";
import { HomePageContext } from "../../Context/HomePageContext";
import { apiBaseUrl } from "../../config/Config";

const FeaturedGame = () => {
  const FETCH_STATUS = {
    IDLE: "idle",
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.IDLE);
  // const [featuredGame, setFeaturedGame] = useState(null);
  const { featuredGame, setFeaturedGame } = useContext(HomePageContext);

  let ranOnce = false;

  const fetchFeaturedGame = async () => {
    if (featuredGame != null) return;
    setFetchStatus(FETCH_STATUS.LOADING);
    try {
      const url = `${apiBaseUrl}/ss/featured-match`;
      const featuredGameRes = await axios.get(url);
      if (featuredGameRes && featuredGameRes.data) {
        setFeaturedGame(featuredGameRes.data);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchFeaturedGame();
      ranOnce = true;
    }
  }, []);

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
    featuredGame && (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <div className="title-style">Featured Game</div>
          <div className="center">
            <div className="featured-game-container">
              <div className="featured-game-div">
                <div className="featured-team-data-wrapper">
                  <div className="featured-team-img-wrapper">
                    <div className="featured-team-img-div">
                      <img
                        src={featuredGame.homeTeamImgUrl}
                        alt=""
                        width={60}
                        height={60}
                      />
                      <div className="featured-team-name-div">
                        <bdi className="featured-team-name">
                          {featuredGame.homeTeamName}
                        </bdi>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="featured-game-date-wrapper">
                  <span className="featured-game-date-time">
                    {String(
                      new Date(featuredGame.startDatetime + "Z").getHours()
                    ).padStart(2, "0") +
                      ":" +
                      String(
                        new Date(featuredGame.startDatetime + "Z").getMinutes()
                      ).padStart(2, "0")}
                  </span>
                  <span className="featured-game-date">
                    {new Date(
                      featuredGame.startDatetime + "Z"
                    ).toLocaleDateString("zh-Hans-CN")}
                  </span>
                </div>
                <div className="featured-team-data-wrapper">
                  <div className="featured-team-img-wrapper">
                    <div className="featured-team-img-div">
                      <img
                        src={featuredGame.awayTeamImgUrl}
                        alt=""
                        width={60}
                        height={60}
                      />
                      <div className="featured-team-name-div">
                        <bdi className="featured-team-name">
                          {featuredGame.awayTeamName}
                        </bdi>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </Box>
    )
  );
};

export default FeaturedGame;
