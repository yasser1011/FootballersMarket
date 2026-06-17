import { Button, CircularProgress } from "@mui/material";
import React, { useContext, useEffect, useState } from "react";
import "./PlayerDetails.css";
import axios from "axios";
import { TransactionConfirmModal } from "./TransactionConfirmModal";
import { UserContext } from "../Context/UserContext";
import { apiBaseUrl } from "../config/Config";

const TransactionButton = ({
  player,
  setPlayerDetails,
  setOuterTransactionConfirmNotificationOpen,
  transactionTypes,
  transactionType,
  setTransactionType,
}) => {
  const [confirmTransactionModalOpen, setConfirmTransactionModalOpen] =
    useState(false);

  const handleConfirmTransactionModalOpen = () => {
    setConfirmTransactionModalOpen(true);
  };

  const openModal = () => {
    handleConfirmTransactionModalOpen();
  };
  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);

  const { userData } = useContext(UserContext);

  let ranOnce = false;

  const fetchPlayerTransactionType = async () => {
    // if no user logged it's a buy button
    if (!userData.user) {
      setFetchStatus(FETCH_STATUS.SUCCESS);
      setTransactionType(transactionTypes.buy);
      return;
    }

    // .id is the rapid api player id
    // players who are not in database are automatic buy
    // if they're already bought, there must be an id recorded for them in database
    if (!player.id) {
      setFetchStatus(FETCH_STATUS.SUCCESS);
      setTransactionType(transactionTypes.buy);
      return;
    }
    try {
      const transactionTypeRes = await axios.get(
        `${apiBaseUrl}/users/player-transaction-type?playerId=${player.id}`,
        { headers: { Authorization: userData.token } },
      );
      if (transactionTypeRes && transactionTypeRes.data) {
        setFetchStatus(FETCH_STATUS.SUCCESS);
        if (transactionTypeRes.data.playerCurrentlyBoughtByUser) {
          let playerSellTransactionType = transactionTypes.sell;
          playerSellTransactionType.priceBought =
            transactionTypeRes.data.buyPrice;
          // when it was bought, so the modal can compute the 48h sell-lock countdown
          playerSellTransactionType.boughtAt = transactionTypeRes.data.boughtAt;
          setTransactionType(playerSellTransactionType);
        } else setTransactionType(transactionTypes.buy);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchPlayerTransactionType();
      ranOnce = true;
    }
  }, [userData.token, player.id]); // to fetch when user logs in in the same page

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return <CircularProgress />;
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return <div style={{ color: "white" }}>Error Please Try Again later</div>;
  }

  return (
    transactionType && (
      <>
        <TransactionConfirmModal
          confirmTransactionModalOpen={confirmTransactionModalOpen}
          setConfirmTransactionModalOpen={setConfirmTransactionModalOpen}
          transactionType={transactionType}
          setTransactionType={setTransactionType}
          transactionTypes={transactionTypes}
          playerDetails={player}
          setPlayerDetails={setPlayerDetails}
          setOuterTransactionConfirmNotificationOpen={
            setOuterTransactionConfirmNotificationOpen
          }
        />
        <Button onClick={openModal} variant="contained" size="medium" fullWidth>
          {transactionType.name}
        </Button>
      </>
    )
  );
};

export default TransactionButton;
