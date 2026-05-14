import React, { useContext, useEffect, useState } from "react";
import "./PlayerDetails.css";
import { Box, Button, Card, CircularProgress, Typography } from "@mui/material";
import { useLocation, useParams } from "react-router-dom";
import axios from "axios";
import { useOnMountUnsafe } from "../customhooks/useOnMountUnsafe";
import { getAge } from "../home/HomeTable";
import TransactionButton from "./TransactionButton";
import RemoveCircleOutlineIcon from "@mui/icons-material/RemoveCircleOutline";
import ArrowDropDownSharpIcon from "@mui/icons-material/ArrowDropDownSharp";
import ArrowDropUpSharpIcon from "@mui/icons-material/ArrowDropUpSharp";
import HorizontalRuleSharpIcon from "@mui/icons-material/HorizontalRuleSharp";
import { apiBaseUrl } from "../config/Config";
import { SelectedHomeTablePlayerContext } from "../Context/SelectedHomeTablePlayerContext";

export const PlayerDetailsInfo = ({
  setOuterTransactionConfirmNotificationOpen,
  setSelectedPlayerSofascoreId,
  FETCH_STATUS,
  fetchStatus,
  setFetchStatus,
  setPlayerLastRatings,
}) => {
  const { pathname } = useLocation();
  const { id } = useParams();
  const { selectedHomeTablePlayer } = useContext(
    SelectedHomeTablePlayerContext,
  );

  const [playerDetails, setPlayerDetails] = useState();

  const transactionTypes = {
    buy: { name: "BUY", value: 1 },
    sell: { name: "SELL", value: 2, priceBought: 0 },
  };
  const [transactionType, setTransactionType] = useState(null);

  let ranOnce = false;

  const fetchPlayerDetails = async (playerExternalId) => {
    try {
      const playerDetRes = await axios.get(
        `${apiBaseUrl}/ss/players?externalId=${playerExternalId}`,
      );
      if (playerDetRes && playerDetRes.data) {
        setPlayerDetails(playerDetRes.data);
        setPlayerLastRatings(playerDetRes.data.recentMatches);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
      throw error;
    }
  };

  const fetchPlayerLastRatings = async (playerExternalId) => {
    try {
      setFetchStatus(FETCH_STATUS.LOADING);

      const playerDetRes = await axios.get(
        `${apiBaseUrl}/ss/players/last-ratings?externalId=${playerExternalId}`,
      );
      if (playerDetRes && playerDetRes.data) {
        setFetchStatus(FETCH_STATUS.SUCCESS);
        setPlayerLastRatings(playerDetRes.data);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
      throw error;
    }
  };

  const getPlayerDetails = () => {
    // last condition if we're in player's page and we used to search for another player from search bar
    // state would still be having old player data and won't be null so won't fetch newew player's data
    if (
      selectedHomeTablePlayer != null &&
      selectedHomeTablePlayer.areClubStatsUpdated &&
      String(selectedHomeTablePlayer.externalServicePlayerId) === String(id)
    ) {
      setPlayerDetails(selectedHomeTablePlayer);
      fetchPlayerLastRatings(id);
    } else {
      fetchPlayerDetails(id);
    }
  };

  useEffect(() => {
    console.log("player details use effect");

    if (!ranOnce) {
      console.log("player details use effect ran once");
      getPlayerDetails();
      ranOnce = true;
    }
  }, [id]);

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <div>
        <Box>
          <Card
            style={{ width: "52rem", height: "25rem", borderRadius: "17px" }}
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
            style={{ width: "52rem", height: "25rem", borderRadius: "17px" }}
            variant="outlined"
          >
            Error Please Try again later
          </Card>
        </Box>
      </div>
    );
  }

  return (
    playerDetails && (
      <div>
        <Box>
          <div className="player-details-info-main-card" variant="outlined">
            <div className="player-details-header">
              <div className="player-details-header-content">
                <div className="player-details-header-content-name-wrapper">
                  <img
                    src={playerDetails.photoUrl}
                    alt=""
                    width={100}
                    height={100}
                    style={{ borderRadius: "50%" }}
                  />
                  <div className="player-details-header-content-name">
                    <Typography
                      variant="h4"
                      style={{
                        color: "white",
                      }}
                    >
                      {playerDetails.name}
                    </Typography>
                    <div className="player-details-header-content-club-wrapper">
                      <img
                        src={playerDetails.clubPhotoUrl}
                        alt=""
                        width={30}
                        height={30}
                        style={{ borderRadius: "50%" }}
                      />
                      <Typography style={{ color: "white" }} variant="h6">
                        {playerDetails.externalServicePlayerClub}
                      </Typography>
                    </div>
                  </div>
                </div>
                <div style={{ width: "7rem" }}>
                  <TransactionButton
                    player={playerDetails}
                    setPlayerDetails={setPlayerDetails}
                    setOuterTransactionConfirmNotificationOpen={
                      setOuterTransactionConfirmNotificationOpen
                    }
                    transactionTypes={transactionTypes}
                    transactionType={transactionType}
                    setTransactionType={setTransactionType}
                  />
                </div>
              </div>
            </div>

            <div className="player-details-content-wrapper">
              <div className="player-details-content-section-wrapper">
                <div className="player-details-league-stats-title">
                  General Info
                </div>
                <div className="player-details-content-grid-style">
                  <div>
                    <div className="player-details-header-title">Rating</div>
                    <div>{playerDetails.avgRating}</div>
                  </div>
                  <div>
                    <div className="player-details-header-title">Price</div>
                    <div>{playerDetails.price}</div>
                  </div>
                  <div>
                    <div className="player-details-header-title">Age</div>
                    <div>{getAge(playerDetails.dateOfBirth)}</div>
                  </div>
                  <div>
                    <div className="player-details-header-title">
                      Nationality
                    </div>
                    <div>{playerDetails.nationality}</div>
                  </div>
                  <div>
                    <div className="player-details-header-title">Position</div>
                    <div>{playerDetails.position}</div>
                  </div>
                  <div>
                    <div className="player-details-header-title">Injured</div>
                    <div>{playerDetails.currentlyInjured ? "Yes" : "No"}</div>
                  </div>
                </div>
              </div>
              <div className="vertical-line"></div>
              <div className="player-details-content-section-wrapper">
                <div className="player-details-league-stats-title">
                  Total League Stats
                </div>

                <div className="player-details-content-grid-style">
                  <div>
                    <div className="player-details-header-title">
                      Total Games
                    </div>
                    <div>
                      {playerDetails.leagueStats &&
                        playerDetails.leagueStats.totalNumOfGames}
                    </div>
                  </div>
                  <div>
                    <div className="player-details-header-title">
                      Total Goals
                    </div>
                    <div>
                      {playerDetails.leagueStats &&
                        playerDetails.leagueStats.goals}
                    </div>
                  </div>
                  <div>
                    <div className="player-details-header-title">
                      Total Assists
                    </div>
                    <div>
                      {playerDetails.leagueStats &&
                        playerDetails.leagueStats.assists}
                    </div>
                  </div>
                  {transactionType &&
                    transactionType.name === transactionTypes.sell.name && (
                      <div>
                        <div className="player-details-header-title">
                          Bought Price
                        </div>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          {transactionType.priceBought}
                          {"   "}
                          {transactionType.priceBought > playerDetails.price ? (
                            <ArrowDropDownSharpIcon />
                          ) : transactionType.priceBought <
                            playerDetails.price ? (
                            <ArrowDropUpSharpIcon />
                          ) : (
                            <HorizontalRuleSharpIcon />
                          )}
                        </div>
                      </div>
                    )}
                </div>
              </div>
            </div>
          </div>
        </Box>
      </div>
    )
  );
};
