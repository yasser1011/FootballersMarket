import React, { useEffect, useState } from "react";
import "./PlayerLastRatings.css";
import { Box, Card, CircularProgress } from "@mui/material";
import axios from "axios";

const PlayerLastRatings = ({
  selectedPlayerSofascoreId,
  FETCH_STATUS,
  fetchStatus,
  setFetchStatus,
  playerLastRatings,
  fetchExternallyStatus,
}) => {
  let ranOnce = false;
  const [playerRatings, setPlayerRatings] = useState([]);

  // const fetchPlayerRatings = async () => {
  //   if (!selectedPlayerSofascoreId) return;

  //   try {
  //     const playerLatestRatingsRes = await axios.get(
  //       `http://localhost:8083/api/ss/players/recent-ratings/${selectedPlayerSofascoreId}`
  //     );
  //     if (playerLatestRatingsRes && playerLatestRatingsRes.data) {
  //       setPlayerRatings(playerLatestRatingsRes.data);

  //       setFetchStatus(FETCH_STATUS.SUCCESS);
  //     }
  //   } catch (error) {
  //     setFetchStatus(FETCH_STATUS.ERROR);
  //   }
  // };

  // useEffect(() => {
  //   if (!ranOnce) {
  //     fetchPlayerRatings();
  //     ranOnce = true;
  //   }
  // }, [selectedPlayerSofascoreId]);

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <div>
        <Box>
          <Card
            style={{ width: "25rem", height: "25rem", borderRadius: "17px" }}
            variant="outlined"
          >
            <CircularProgress />
          </Card>
        </Box>
      </div>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <div>
        <Box>
          <Card
            style={{ width: "25rem", height: "25rem", borderRadius: "17px" }}
            variant="outlined"
          >
            Error Please Try again later
          </Card>
        </Box>
      </div>
    );
  }

  return (
    playerLastRatings && (
      <Box>
        <Card
          style={{ width: "25rem", height: "25rem", borderRadius: "17px" }}
          variant="outlined"
        >
          <div className="title-style">Latest Ratings</div>
          <div style={{ marginTop: "70px" }}>
            <div className="player-latest-avg-rating-wrapper">
              <div className="player-latest-avg-rating-title">
                Average Rating
              </div>
              <div className="rating-item-number-style">
                {playerLastRatings.length > 0 &&
                  (
                    playerLastRatings.reduce((a, b) => a + b.rating, 0) /
                    playerLastRatings.length
                  ).toFixed(2)}
              </div>
            </div>
            <div className="vertical-line-horizontal-wrapper">
              <div className="vertical-line-horizontal"></div>
              <div className="ratings-container">
                {playerLastRatings.map((rating, idx) => {
                  return (
                    <div key={idx} className="ratings-item">
                      <div className="rating-item-number-style">
                        {rating.rating}
                      </div>
                      <img
                        src={rating.opponentTeamPhotoUrl}
                        width={30}
                        height={30}
                        style={{ borderRadius: "50%" }}
                        alt=""
                      />
                      <div>
                        {rating.opponentTeamName.split(" ")[0].substring(0, 3)}
                      </div>
                      <div className="rating-item-date-style">
                        {new Date(rating.startDate).toLocaleString("en-us", {
                          day: "numeric",
                          month: "short",
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </Card>
      </Box>
    )
  );
};

export default PlayerLastRatings;
