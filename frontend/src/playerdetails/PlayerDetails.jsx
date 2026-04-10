import { Box, Button, Card, Snackbar, Typography } from "@mui/material";
import React, { useState } from "react";
import "./PlayerDetails.css";
import PlayerLastRatings from "./PlayerLastRatings";
import { PlayerDetailsInfo } from "./PlayerDetailsInfo";

const PlayerDetails = () => {
  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [selectedPlayerSofascoreId, setSelectedPlayerSofascoreId] =
    useState("");

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);
  const [playerLastRatings, setPlayerLastRatings] = useState();

  const [
    outerTransactionConfirmNotificationOpen,
    setOuterTransactionConfirmNotificationOpen,
  ] = useState({ isOpen: false, msg: "" });
  return (
    <div className="player-details-info-container">
      <div className="player-details-info-wrapper">
        <PlayerDetailsInfo
          setOuterTransactionConfirmNotificationOpen={
            setOuterTransactionConfirmNotificationOpen
          }
          setSelectedPlayerSofascoreId={setSelectedPlayerSofascoreId}
          FETCH_STATUS={FETCH_STATUS}
          fetchStatus={fetchStatus}
          setFetchStatus={setFetchStatus}
          setPlayerLastRatings={setPlayerLastRatings}
        />
        <PlayerLastRatings
          selectedPlayerSofascoreId={selectedPlayerSofascoreId}
          FETCH_STATUS={FETCH_STATUS}
          fetchStatus={fetchStatus}
          setFetchStatus={setFetchStatus}
          playerLastRatings={playerLastRatings}
        />
      </div>
      <Snackbar
        open={outerTransactionConfirmNotificationOpen.isOpen}
        message={outerTransactionConfirmNotificationOpen.msg}
        autoHideDuration={3000}
        onClose={() => setOuterTransactionConfirmNotificationOpen(false)}
      />
    </div>
  );
};

export default PlayerDetails;
