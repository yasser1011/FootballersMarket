import React, { useEffect, useState } from "react";
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

export const PlayerDetailsInfo = ({
  setOuterTransactionConfirmNotificationOpen,
  setSelectedPlayerSofascoreId,
  FETCH_STATUS,
  fetchStatus,
  setFetchStatus,
  setPlayerLastRatings,
  fetchinternallyStatus,
  fetchExternallyStatus,
  setFetchExternallyStatus,
  setFetchInternallyStatus,
}) => {
  const { state, pathname } = useLocation();
  const { id } = useParams();

  const [playerDetails, setPlayerDetails] = useState();

  const transactionTypes = {
    buy: { name: "BUY", value: 1 },
    sell: { name: "SELL", value: 2, priceBought: 0 },
  };
  const [transactionType, setTransactionType] = useState(null);

  let ranOnce = false;

  const fetchPlayerDetailsExternal = async (urlParams) => {
    try {
      setFetchExternallyStatus(FETCH_STATUS.LOADING);
      const playerDetRes = await axios.get(
        `${apiBaseUrl}/ss/players?${urlParams}`
      );
      if (playerDetRes && playerDetRes.data) {
        setFetchExternallyStatus(FETCH_STATUS.SUCCESS);
        return playerDetRes.data;
      }
      throw new Error("No data received");
    } catch (error) {
      setFetchExternallyStatus(FETCH_STATUS.ERROR);
      throw error;
    }
  };

  const fetchPlayerDetailsInternal = async (playerId) => {
    try {
      setFetchInternallyStatus(FETCH_STATUS.LOADING);
      const playerDetRes = await axios.get(`${apiBaseUrl}/players/${playerId}`);
      if (playerDetRes && playerDetRes.data) {
        setPlayerDetails(playerDetRes.data);
        setSelectedPlayerSofascoreId(playerDetRes.data.externalServicePlayerId);
        let urlParams = `sofascoreId=${playerDetRes.data.externalServicePlayerId}`;
        // need to fetch last ratings
        fetchPlayerLastRatings(urlParams);
        // setFetchStatus(FETCH_STATUS.SUCCESS);
        setFetchInternallyStatus(FETCH_STATUS.SUCCESS);
      }
      throw new Error("No data received");
    } catch (error) {
      // setFetchStatus(FETCH_STATUS.ERROR);
      setFetchInternallyStatus(FETCH_STATUS.ERROR);
      throw error;
    }
  };

  const setExternalPlayerDetailsState = async (urlParams) => {
    try {
      let playerDetRes = await fetchPlayerDetailsExternal(urlParams);
      setPlayerDetails(playerDetRes);
      setSelectedPlayerSofascoreId(playerDetRes.externalServicePlayerId);

      setPlayerLastRatings(playerDetRes.recentMatches);
      // setFetchStatus(FETCH_STATUS.SUCCESS);
    } catch (error) {
      console.log("error");
      // setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  const fetchPlayerLastRatings = async (urlParams) => {
    try {
      let playerDetRes = await fetchPlayerDetailsExternal(urlParams);
      // setPlayerDetails(playerDetRes);
      setPlayerLastRatings(playerDetRes.recentMatches);
      setSelectedPlayerSofascoreId(playerDetRes.externalServicePlayerId);
      // setFetchStatus(FETCH_STATUS.SUCCESS);
    } catch (error) {
      console.log("error");
      // setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  const fetchPlayerDetails = () => {
    // url is /rp/:id
    // 1- if player state is available and club stats is updated -> render player details as is
    // 2- if player state is available and club stats is not updated -> get details from sofascore by sofascore id param
    // and add updated by sofascore
    // 3- if player state not available -> fetch by id from db -> if club stats not updated
    // get player details from sofascore by sofascore id and update data in db from sofascore details and add updated by
    // url is /sf/:id
    // 1- get his stats as a view only from sofascore by sofascore id in url
    // console.log("test all");
    if (pathname.includes("/rp/")) {
      if (state == null) {
        // get from player service by rapid id
        fetchPlayerDetailsInternal(id);
      } else if (
        // have the correct player data but need to fetch latest ratings
        state.srcLocation === "search" ||
        state.player.areClubStatsUpdated
      ) {
        let urlParams = `sofascoreId=${state.player.externalServicePlayerId}`;
        setPlayerDetails(state.player);
        fetchPlayerLastRatings(urlParams);
        setSelectedPlayerSofascoreId(state.player.externalServicePlayerId);
      } else if (!state.player.areClubStatsUpdated) {
        // get from sofascore service by sofascore id
        // means he's in db but data is not updated

        let urlParams = `sofascoreId=${state.player.externalServicePlayerId}&rapidId=${state.player.id}`;
        setExternalPlayerDetailsState(urlParams); // need to just set player last ratings in corresponding state
      }
    } else {
      // get from sofascore service for view only
      let urlParams = `sofascoreId=${id}`;
      setExternalPlayerDetailsState(urlParams); // need to just set player last ratings in corresponding state
    }
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchPlayerDetails();
      ranOnce = true;
    }
  }, [id]);

  if (
    fetchinternallyStatus === FETCH_STATUS.LOADING ||
    fetchExternallyStatus === FETCH_STATUS.LOADING
  ) {
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

  if (
    fetchinternallyStatus === FETCH_STATUS.ERROR ||
    fetchExternallyStatus === FETCH_STATUS.ERROR
  ) {
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
