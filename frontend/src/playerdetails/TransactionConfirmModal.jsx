import {
  Box,
  Button,
  CircularProgress,
  Modal,
  Typography,
} from "@mui/material";
import React, { useContext, useState } from "react";
import DoneOutlinedIcon from "@mui/icons-material/DoneOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import useMediaQuery from "@mui/material/useMediaQuery";
import axios from "axios";
import { UserContext } from "../Context/UserContext";
import { apiBaseUrl } from "../config/Config";

export const TransactionConfirmModal = ({
  confirmTransactionModalOpen,
  setConfirmTransactionModalOpen,
  transactionType,
  setTransactionType,
  transactionTypes,
  playerDetails,
  setPlayerDetails,
  setOuterTransactionConfirmNotificationOpen,
}) => {
  const style = {
    position: "absolute",
    top: "50%",
    left: "50%",
    transform: "translate(-50%, -50%)",
    width: useMediaQuery("(min-width:600px)") ? 500 : 380,
    minHeight: 100,
    bgcolor: "background.paper",
    border: "2px solid #000",
    boxShadow: 24,
    p: 4,
  };

  const { userData, setUserData } = useContext(UserContext);

  const handleClose = () => {
    setConfirmTransactionModalOpen(false);
  };

  // one message for success and one for fail
  const TRANSACTION_STATUSES = {
    IDLE: { name: "idle" },
    ERROR: { name: "error", msg: "Error Please try again later" },
    SUCCESS: { name: "success", msg: "Transaction Done Successfully" },
    LOADING: { name: "loading" },
  };

  const [confirmTransactionBtnDisabled, setConfirmTransactionBtnDisabled] =
    useState(false);
  const [transactionStatus, setTransactionStatus] = useState(
    TRANSACTION_STATUSES.IDLE
  );

  const SELL_LOCK_HOURS = 48; // mirrors backend TransactionService.SELL_LOCK_HOURS

  // how long until a freshly bought player can be sold, derived from boughtAt
  const sellWaitText = (boughtAt) => {
    if (!boughtAt) return "48 hours after buying";
    const unlockMs =
      new Date(boughtAt).getTime() + SELL_LOCK_HOURS * 60 * 60 * 1000;
    const msLeft = unlockMs - Date.now();
    if (msLeft <= 0) return "now";
    const hours = Math.floor(msLeft / (60 * 60 * 1000));
    const minutes = Math.floor((msLeft % (60 * 60 * 1000)) / (60 * 1000));
    return `${hours}h ${minutes}m`;
  };

  // human message for each backend transaction error type (TransactionError.errorType)
  const messageForError = (err) => {
    switch (err.errorType) {
      case "max_buy":
        return "Your squad is full — you can hold at most 4 players. Sell one first.";
      case "sell_locked":
        return `You can't sell this player yet — try again in ${sellWaitText(
          transactionType.boughtAt
        )}.`;
      case "live_match":
        return "This player is in a live match right now — you can buy him after it ends.";
      case "already_owned":
        return "You already own this player.";
      case "insufficient_points":
        return "You don't have enough points for this player.";
      case "not_owned":
        return "You don't own this player.";
      default:
        return "Error Please try again later";
    }
  };

  const handleTransactionErr = (transactionResponse) => {
    setTransactionStatus(TRANSACTION_STATUSES.ERROR);
    setTimeout(() => {
      setTransactionStatus(TRANSACTION_STATUSES.IDLE);
      setConfirmTransactionBtnDisabled(false);
      handleClose();
      const err =
        transactionResponse && transactionResponse.error
          ? transactionResponse.error
          : null;
      if (err && err.errorType === "price") {
        setPlayerDetails((prevState) => ({
          ...prevState,
          price: err.playerPrice,
        }));
        setOuterTransactionConfirmNotificationOpen({
          isOpen: true,
          msg: "Sorry Price was updated",
        });
      } else {
        setOuterTransactionConfirmNotificationOpen({
          isOpen: true,
          msg: err ? messageForError(err) : "Error Please try again later",
        });
      }
    }, 2000);
  };

  const makeTransaction = async () => {
    let userPoints = userData.user.points;
    if (
      playerDetails.price > userPoints &&
      transactionType.name === transactionTypes.buy.name
    ) {
      handleClose();
      setOuterTransactionConfirmNotificationOpen({
        isOpen: true,
        msg: "Sorry, Price is higher than your points",
      });
      return;
    }

    // set transaction loading
    setConfirmTransactionBtnDisabled(true);
    setTransactionStatus(TRANSACTION_STATUSES.LOADING);

    // setTimeout(() => {

    // }, 2000);
    const transactionReqData = {
      // userId: 1,
      playerId: playerDetails.id,
      playerSofascoreId: playerDetails.externalServicePlayerId,
      price: playerDetails.price,
      transactionType: transactionType.value,
    };

    try {
      await axios.post(
        `${apiBaseUrl}/transactions`,
        transactionReqData,
        { headers: { Authorization: userData.token } }
      );

      setTransactionStatus(TRANSACTION_STATUSES.SUCCESS);
      if (transactionType.name === transactionTypes.buy.name) {
        setUserData((prevState) => {
          return {
            ...prevState,
            user: {
              ...prevState.user,
              points: prevState.user.points - playerDetails.price,
            },
          };
        });
      } else {
        setUserData((prevState) => {
          return {
            ...prevState,
            user: {
              ...prevState.user,
              points: prevState.user.points + playerDetails.price,
            },
          };
        });
      }

      setTimeout(() => {
        setTransactionStatus(TRANSACTION_STATUSES.IDLE);
        setConfirmTransactionBtnDisabled(false);
        handleClose();
        setOuterTransactionConfirmNotificationOpen({
          isOpen: true,
          msg: "Transaction Done Successfully",
        });
        // change the main buy button to sell or sell to buy after successful transaction
        setTransactionType((oldType) => {
          if (oldType.name === transactionTypes.buy.name) {
            let newSellTransactionType = transactionTypes.sell;
            newSellTransactionType.priceBought = playerDetails.price;
            return newSellTransactionType;
          } else {
            return transactionTypes.buy;
          }
        });
      }, 3000);
    } catch (error) {
      console.log(error);

      handleTransactionErr(error.response && error.response.data);
    }
  };

  if (!userData.user) {
    return (
      <Modal
        open={confirmTransactionModalOpen}
        onClose={handleClose}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <Box sx={style}>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-around",
              // backgroundColor: "red",
              height: "100%",
            }}
          >
            <Typography id="modal-modal-title" variant="h5" component="h2">
              Note
            </Typography>
            <Typography id="modal-modal-title">
              Please Login to to be able to Buy and Sell
            </Typography>
          </div>
        </Box>
      </Modal>
    );
  }

  return (
    <div>
      <Modal
        open={confirmTransactionModalOpen}
        onClose={handleClose}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <Box sx={style}>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-around",
              // backgroundColor: "red",
              height: "100%",
            }}
          >
            <Typography id="modal-modal-title" variant="h5" component="h2">
              Confirm
            </Typography>
            <Typography id="modal-modal-title">
              Are you sure you want to {transactionType.name}{" "}
              {playerDetails.name} for {playerDetails.price}
            </Typography>
            {transactionType.name === transactionTypes.buy.name && (
              <Typography
                style={{ fontSize: "0.8rem", color: "#666", marginTop: "6px" }}
              >
                Note: you can hold at most 4 players, and a player can't be sold
                until 48 hours after you buy him.
              </Typography>
            )}
            <div
              style={{
                display: "flex",
                justifyContent: "space-around",
                alignItems: "right",
                marginTop: "30px",
              }}
            >
              {(transactionStatus.name === "success" ||
                transactionStatus.name === "error") && (
                <div
                  style={{
                    width: "300px",
                    backgroundColor: "lightgray",
                    display: "flex",
                    justifyContent: "left",
                    alignItems: "center",
                    borderRadius: "5px",
                  }}
                >
                  {transactionStatus.name === "success" ? (
                    <DoneOutlinedIcon
                      style={{
                        backgroundColor: "green",
                        borderRadius: "50%",
                        marginRight: "8px",
                        marginLeft: "5px",
                      }}
                    />
                  ) : (
                    <CloseOutlinedIcon
                      style={{
                        backgroundColor: "green",
                        borderRadius: "50%",
                        marginRight: "8px",
                        marginLeft: "5px",
                      }}
                    />
                  )}

                  <div>{transactionStatus.msg}</div>
                </div>
              )}

              <div>
                <Button
                  style={{ width: "80px" }}
                  variant="contained"
                  size="small"
                  disabled={confirmTransactionBtnDisabled}
                  onClick={makeTransaction}
                >
                  {transactionStatus.name === "loading" ? (
                    <CircularProgress size="1.4rem" />
                  ) : (
                    "Confirm"
                  )}
                </Button>
                <Button
                  style={{ width: "80px", marginLeft: "10px" }}
                  variant="contained"
                  size="small"
                  color="error"
                  onClick={handleClose}
                  disabled={confirmTransactionBtnDisabled}
                >
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        </Box>
      </Modal>
    </div>
  );
};
