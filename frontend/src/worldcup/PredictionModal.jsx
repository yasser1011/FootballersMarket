import React, { useContext, useState } from "react";
import {
  Box,
  Button,
  CircularProgress,
  Modal,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import axios from "axios";
import { apiBaseUrl } from "../config/Config";
import { UserContext } from "../Context/UserContext";

// winner pick + optional exact score for a single fixture. POSTs to /api/wc/predictions and
// surfaces the backend's prediction errors (closed after kickoff, validation, auth).
const STATUS = { IDLE: "idle", LOADING: "loading", ERROR: "error" };

const modalStyle = {
  position: "absolute",
  top: "50%",
  left: "50%",
  transform: "translate(-50%, -50%)",
  width: 420,
  maxWidth: "90vw",
  bgcolor: "background.paper",
  borderRadius: "10px",
  boxShadow: 24,
  p: 3,
};

const PredictionModal = ({ fixture, existingPrediction, onClose, onSaved }) => {
  const { userData } = useContext(UserContext);

  const initialChoice = (() => {
    if (!existingPrediction) return null;
    if (existingPrediction.predictedWinnerTeamId == null) return "draw";
    return existingPrediction.predictedWinnerTeamId === fixture.home?.id
      ? "home"
      : "away";
  })();

  const [choice, setChoice] = useState(initialChoice); // 'home' | 'draw' | 'away'
  const [homeGoals, setHomeGoals] = useState(
    existingPrediction?.predictedHomeGoals ?? "",
  );
  const [awayGoals, setAwayGoals] = useState(
    existingPrediction?.predictedAwayGoals ?? "",
  );
  const [status, setStatus] = useState(STATUS.IDLE);
  const [errorMsg, setErrorMsg] = useState("");

  const submit = async () => {
    if (!userData.user) {
      setStatus(STATUS.ERROR);
      setErrorMsg("Please sign in to predict.");
      return;
    }
    if (!choice) {
      setStatus(STATUS.ERROR);
      setErrorMsg("Pick a result first.");
      return;
    }

    const predictedWinnerTeamId =
      choice === "draw"
        ? null
        : choice === "home"
          ? fixture.home?.id
          : fixture.away?.id;

    const body = {
      fixtureId: fixture.id,
      predictedWinnerTeamId,
      predictedHomeGoals: homeGoals === "" ? null : Number(homeGoals),
      predictedAwayGoals: awayGoals === "" ? null : Number(awayGoals),
    };

    setStatus(STATUS.LOADING);
    setErrorMsg("");
    try {
      await axios.post(`${apiBaseUrl}/wc/predictions`, body, {
        headers: { Authorization: userData.token },
      });
      onSaved();
    } catch (error) {
      setStatus(STATUS.ERROR);
      const code = error.response && error.response.status;
      const serverMsg =
        error.response && typeof error.response.data === "string"
          ? error.response.data
          : null;
      if (code === 409) {
        setErrorMsg("This match has already started — predictions are closed.");
      } else if (code === 400) {
        setErrorMsg(serverMsg || "Invalid prediction.");
      } else if (code === 401 || code === 403) {
        setErrorMsg("Please sign in to predict.");
      } else {
        setErrorMsg("Something went wrong, please try again.");
      }
    }
  };

  return (
    <Modal open onClose={onClose}>
      <Box sx={modalStyle}>
        <Typography variant="h6">Predict result</Typography>
        <Typography style={{ fontSize: "0.9rem", margin: "4px 0 14px" }}>
          {fixture.home?.name} vs {fixture.away?.name}
        </Typography>

        <ToggleButtonGroup
          exclusive
          fullWidth
          size="small"
          value={choice}
          onChange={(e, value) => value && setChoice(value)}
        >
          <ToggleButton value="home">{fixture.home?.name}</ToggleButton>
          <ToggleButton value="draw">Draw</ToggleButton>
          <ToggleButton value="away">{fixture.away?.name}</ToggleButton>
        </ToggleButtonGroup>

        <div
          style={{
            display: "flex",
            gap: 10,
            marginTop: 16,
            alignItems: "center",
          }}
        >
          <TextField
            type="number"
            label="Home goals"
            size="small"
            value={homeGoals}
            onChange={(e) => setHomeGoals(e.target.value)}
            inputProps={{ min: 0 }}
          />
          <span>-</span>
          <TextField
            type="number"
            label="Away goals"
            size="small"
            value={awayGoals}
            onChange={(e) => setAwayGoals(e.target.value)}
            inputProps={{ min: 0 }}
          />
        </div>
        <Typography style={{ fontSize: "0.75rem", color: "#666", marginTop: 8 }}>
          Score is optional — enter both or neither. An exact score earns a bonus
          and must match your pick.
        </Typography>

        {status === STATUS.ERROR && (
          <Typography
            style={{ color: "#d32f2f", fontSize: "0.82rem", marginTop: 10 }}
          >
            {errorMsg}
          </Typography>
        )}

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 10,
            marginTop: 20,
          }}
        >
          <Button
            size="small"
            color="error"
            variant="contained"
            onClick={onClose}
            disabled={status === STATUS.LOADING}
          >
            Cancel
          </Button>
          <Button
            size="small"
            variant="contained"
            onClick={submit}
            disabled={status === STATUS.LOADING}
          >
            {status === STATUS.LOADING ? (
              <CircularProgress size="1.2rem" />
            ) : (
              "Save"
            )}
          </Button>
        </div>
      </Box>
    </Modal>
  );
};

export default PredictionModal;
