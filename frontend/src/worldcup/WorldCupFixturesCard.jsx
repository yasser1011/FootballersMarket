import React, { useContext, useEffect, useRef, useState } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import {
  Button,
  CircularProgress,
  IconButton,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
} from "@mui/material";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import axios from "axios";
import { apiBaseUrl } from "../config/Config";
import { UserContext } from "../Context/UserContext";
import AuthModal from "../auth/AuthModal";
import { pointsBreakdown, signed } from "./predictionPoints";
import "./WorldCup.css";

const authModalTypes = { SIGNIN: "SIGNIN", SIGNUP: "SIGNUP" };

// World Cup mode header card: one fixture at a time (swap with ‹ ›) with INLINE prediction.
// Upcoming matches show editable controls; once a match has started/ended the controls lock and
// it shows the user's pick + predicted score + settlement result (+/-) next to the live/final score.
const FETCH_STATUS = { LOADING: "loading", SUCCESS: "success", ERROR: "error" };
const FINISHED_STATUSES = ["FT", "AET", "PEN"];
const cardStyle = { borderRadius: "10px", width: "25rem", height: "18rem" };

const WorldCupFixturesCard = () => {
  const { userData } = useContext(UserContext);

  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);
  const [fixtures, setFixtures] = useState([]);
  const [predictionsByFixture, setPredictionsByFixture] = useState({});
  const [index, setIndex] = useState(0);
  const initializedRef = useRef(false);

  // inline prediction form state for the currently shown fixture
  const [choice, setChoice] = useState(null); // 'home' | 'draw' | 'away'
  const [homeGoals, setHomeGoals] = useState("");
  const [awayGoals, setAwayGoals] = useState("");
  const [saveStatus, setSaveStatus] = useState("idle"); // idle | loading | error
  const [saveError, setSaveError] = useState("");

  // sign-in modal (shown when a guest tries to predict)
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalType, setAuthModalType] = useState(null);
  const openSignin = () => {
    setAuthModalType(authModalTypes.SIGNIN);
    setAuthModalOpen(true);
  };

  const fetchFixtures = async () => {
    setFetchStatus(FETCH_STATUS.LOADING);
    try {
      const res = await axios.get(`${apiBaseUrl}/wc/fixtures`);
      const list = res.data || [];
      setFixtures(list);
      if (!initializedRef.current && list.length > 0) {
        const firstActive = list.findIndex(
          (f) => !FINISHED_STATUSES.includes(f.status),
        );
        setIndex(firstActive >= 0 ? firstActive : list.length - 1);
        initializedRef.current = true;
      }
      setFetchStatus(FETCH_STATUS.SUCCESS);
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  const fetchMyPredictions = async () => {
    if (!userData.user) {
      setPredictionsByFixture({});
      return;
    }
    try {
      const res = await axios.get(`${apiBaseUrl}/wc/predictions/mine`, {
        headers: { Authorization: userData.token },
      });
      const map = {};
      (res.data || []).forEach((p) => {
        map[p.fixtureId] = p;
      });
      setPredictionsByFixture(map);
    } catch (error) {
      setPredictionsByFixture({});
    }
  };

  useEffect(() => {
    fetchFixtures();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchMyPredictions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userData.token]);

  const fixture =
    fixtures.length > 0 ? fixtures[Math.min(index, fixtures.length - 1)] : null;
  const prediction = fixture ? predictionsByFixture[fixture.id] : null;

  const isFinished = (f) => FINISHED_STATUSES.includes(f.status);
  const isUpcoming = (f) =>
    !isFinished(f) && new Date(f.date).getTime() > Date.now();
  const isLive = (f) => !isFinished(f) && !isUpcoming(f);
  // locked once the match has started or ended -> can't predict/edit
  const locked = fixture ? !isUpcoming(fixture) : true;

  // sync the form to the shown fixture's existing prediction whenever it changes
  useEffect(() => {
    if (!fixture) return;
    if (prediction) {
      setChoice(
        prediction.predictedWinnerTeamId == null
          ? "draw"
          : prediction.predictedWinnerTeamId === fixture.home?.id
            ? "home"
            : "away",
      );
      setHomeGoals(prediction.predictedHomeGoals ?? "");
      setAwayGoals(prediction.predictedAwayGoals ?? "");
    } else {
      setChoice(null);
      setHomeGoals("");
      setAwayGoals("");
    }
    setSaveStatus("idle");
    setSaveError("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fixture?.id, prediction]);

  const kickoffText = (dateStr) =>
    new Date(dateStr).toLocaleString([], {
      weekday: "short",
      hour: "2-digit",
      minute: "2-digit",
    });

  const statusLabel = (f) => {
    if (isFinished(f)) return "Full time";
    if (isLive(f)) return f.elapsed != null ? `Live ${f.elapsed}'` : "Live";
    return f.round || "";
  };

  const pickName = (p, f) => {
    if (!p) return null;
    if (p.predictedWinnerTeamId == null) return "Draw";
    return p.predictedWinnerTeamId === f.home?.id ? f.home?.name : f.away?.name;
  };

  const submit = async () => {
    if (!userData.user) {
      setSaveStatus("error");
      setSaveError("Please sign in to predict.");
      return;
    }
    let resolvedChoice = choice;
    if (homeGoals !== "" && awayGoals !== "") {
      const h = parseInt(homeGoals, 10);
      const a = parseInt(awayGoals, 10);
      if (!isNaN(h) && !isNaN(a)) {
        if (h > a) resolvedChoice = "home";
        else if (a > h) resolvedChoice = "away";
        else resolvedChoice = "draw";
        setChoice(resolvedChoice);
      }
    }
    if (!resolvedChoice) {
      setSaveStatus("error");
      setSaveError("Pick a result first.");
      return;
    }
    const predictedWinnerTeamId =
      resolvedChoice === "draw"
        ? null
        : resolvedChoice === "home"
          ? fixture.home?.id
          : fixture.away?.id;
    const body = {
      fixtureId: fixture.id,
      predictedWinnerTeamId,
      predictedHomeGoals: homeGoals === "" ? null : Number(homeGoals),
      predictedAwayGoals: awayGoals === "" ? null : Number(awayGoals),
    };
    setSaveStatus("loading");
    setSaveError("");
    try {
      await axios.post(`${apiBaseUrl}/wc/predictions`, body, {
        headers: { Authorization: userData.token },
      });
      await fetchMyPredictions();
      setSaveStatus("idle");
    } catch (error) {
      setSaveStatus("error");
      const code = error.response && error.response.status;
      const serverMsg =
        error.response && typeof error.response.data === "string"
          ? error.response.data
          : null;
      if (code === 409)
        setSaveError("This match has already started — predictions are closed.");
      else if (code === 400) setSaveError(serverMsg || "Invalid prediction.");
      else if (code === 401 || code === 403)
        setSaveError("Please sign in to predict.");
      else setSaveError("Something went wrong, please try again.");
    }
  };

  const prev = () => setIndex((i) => Math.max(0, i - 1));
  const next = () => setIndex((i) => Math.min(fixtures.length - 1, i + 1));

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card style={cardStyle} variant="outlined">
          <div className="center-div" style={{ height: "100%" }}>
            <CircularProgress />
          </div>
        </Card>
      </Box>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card style={cardStyle} variant="outlined">
          <div className="title-style">World Cup Matches</div>
          <div className="wc-fixtures-empty">Error Please Try again Later</div>
        </Card>
      </Box>
    );
  }

  if (!fixture) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card style={cardStyle} variant="outlined">
          <div className="title-style">World Cup Matches</div>
          <div className="wc-fixtures-empty">No matches today or tomorrow</div>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ minWidth: 275 }}>
      <Card style={cardStyle} variant="outlined">
        <div className="title-style">World Cup Matches</div>

        <div className="wc-carousel">
          <IconButton size="small" onClick={prev} disabled={index === 0}>
            <ChevronLeftRoundedIcon />
          </IconButton>

          <div className="wc-carousel-match">
            <div className="wc-carousel-status">{statusLabel(fixture)}</div>
            <div className="wc-carousel-teams">
              <div className="wc-carousel-team">
                <img src={fixture.home?.logoUrl} width={40} height={40} alt="" />
                <span>{fixture.home?.name}</span>
              </div>
              <div className="wc-carousel-center">
                {isUpcoming(fixture) ? (
                  <span className="wc-carousel-time">
                    {kickoffText(fixture.date)}
                  </span>
                ) : (
                  <span className="wc-carousel-score">
                    {fixture.homeGoals ?? "-"} - {fixture.awayGoals ?? "-"}
                  </span>
                )}
              </div>
              <div className="wc-carousel-team">
                <img src={fixture.away?.logoUrl} width={40} height={40} alt="" />
                <span>{fixture.away?.name}</span>
              </div>
            </div>

            <div className="wc-carousel-pred">
              {locked ? (
                // started/ended: show the user's prediction + settlement result
                <div className="wc-pred-summary">
                  {prediction ? (
                    <>
                      <span>
                        Your pick: <b>{pickName(prediction, fixture)}</b>
                        {prediction.predictedHomeGoals != null &&
                          ` (${prediction.predictedHomeGoals}-${prediction.predictedAwayGoals})`}
                      </span>
                      {prediction.settled &&
                      prediction.awardedPoints != null ? (
                        (() => {
                          const bd = pointsBreakdown(prediction);
                          if (!bd.hasParts) {
                            return (
                              <span
                                className={
                                  bd.total >= 0
                                    ? "wc-pred-result-plus"
                                    : "wc-pred-result-minus"
                                }
                              >
                                {signed(bd.total)}
                              </span>
                            );
                          }
                          return (
                            <span className="wc-pred-breakdown">
                              <span className="wc-pred-part">
                                <span className="wc-pred-part-label">Result</span>
                                <span
                                  className={
                                    bd.result >= 0
                                      ? "wc-pred-result-plus"
                                      : "wc-pred-result-minus"
                                  }
                                >
                                  {signed(bd.result)}
                                </span>
                              </span>
                              {bd.exact > 0 && (
                                <span className="wc-pred-part">
                                  <span className="wc-pred-part-label">Exact</span>
                                  <span className="wc-pred-result-plus">
                                    {signed(bd.exact)}
                                  </span>
                                </span>
                              )}
                              {bd.bonus > 0 && (
                                <span className="wc-pred-part">
                                  <span className="wc-pred-part-label">Bonus</span>
                                  <span className="wc-pred-result-plus">
                                    {signed(bd.bonus)}
                                  </span>
                                </span>
                              )}
                              <span className="wc-pred-part wc-pred-part-total">
                                <span className="wc-pred-part-label">Total</span>
                                <span
                                  className={
                                    bd.total >= 0
                                      ? "wc-pred-result-plus"
                                      : "wc-pred-result-minus"
                                  }
                                >
                                  {signed(bd.total)}
                                </span>
                              </span>
                            </span>
                          );
                        })()
                      ) : (
                        <span className="wc-pred-hint">Awaiting result</span>
                      )}
                    </>
                  ) : (
                    <span className="wc-pred-hint">No prediction</span>
                  )}
                </div>
              ) : !userData.user ? (
                <Button size="small" variant="contained" onClick={openSignin}>
                  Sign in to predict
                </Button>
              ) : (
                // upcoming: inline editable prediction
                <>
                  <ToggleButtonGroup
                    exclusive
                    fullWidth
                    size="small"
                    value={choice}
                    onChange={(e, v) => v && setChoice(v)}
                  >
                    <ToggleButton value="home" sx={toggleSx}>
                      {fixture.home?.name}
                    </ToggleButton>
                    <ToggleButton value="draw" sx={toggleSx}>
                      Draw
                    </ToggleButton>
                    <ToggleButton value="away" sx={toggleSx}>
                      {fixture.away?.name}
                    </ToggleButton>
                  </ToggleButtonGroup>
                  <div className="wc-pred-score">
                    <TextField
                      type="number"
                      size="small"
                      value={homeGoals}
                      onChange={(e) => setHomeGoals(e.target.value)}
                      inputProps={{ min: 0, style: { padding: "4px 6px" } }}
                    />
                    <span>-</span>
                    <TextField
                      type="number"
                      size="small"
                      value={awayGoals}
                      onChange={(e) => setAwayGoals(e.target.value)}
                      inputProps={{ min: 0, style: { padding: "4px 6px" } }}
                    />
                    <Button
                      size="small"
                      variant="contained"
                      onClick={submit}
                      disabled={saveStatus === "loading"}
                    >
                      {saveStatus === "loading" ? (
                        <CircularProgress size="1rem" />
                      ) : prediction ? (
                        "Update"
                      ) : (
                        "Predict"
                      )}
                    </Button>
                  </div>
                  {saveStatus === "error" && (
                    <div className="wc-pred-error">{saveError}</div>
                  )}
                </>
              )}
            </div>
          </div>

          <IconButton
            size="small"
            onClick={next}
            disabled={index >= fixtures.length - 1}
          >
            <ChevronRightRoundedIcon />
          </IconButton>
        </div>

        <div className="wc-carousel-counter">
          {index + 1} / {fixtures.length}
        </div>
      </Card>

      {authModalOpen && (
        <AuthModal
          authModalOpen={authModalOpen}
          setAuthModalOpen={setAuthModalOpen}
          authModalTypes={authModalTypes}
          authModalType={authModalType}
          setAuthModalType={setAuthModalType}
        />
      )}
    </Box>
  );
};

const toggleSx = {
  minWidth: 0,
  px: 0.5,
  fontSize: "0.72rem",
  fontWeight: 500,
  lineHeight: 1.1,
  textTransform: "none",
  letterSpacing: "0.01em",
  transition: "background-color 0.15s",
 "&:hover": {
    backgroundColor: "#e0e0e0", // Solid, visible greyish color on hover
    color: "#232f3e",
    boxShadow: "0 2px 5px rgba(0,0,0,0.15)", // Noticeable shadow to make it stand out
  },
"&.Mui-selected": {
    backgroundColor: "#85888b", 
    color: "#ffffff", // Inverts text to white so it's highly visible
    boxShadow: "inset 0 3px 5px rgba(0,0,0,0.3)", // Heavy inset shadow for that "clicked in" look
    
    "&:hover": {
      backgroundColor: "#85888b", // Even darker brand blue if they hover while selected
    },
  },
};


/*
const toggleSx = {
  minWidth: 0,        // Restored your exact original compact width logic
  px: 0.5,           // Restored your exact original padding
  fontSize: "0.66rem",
  lineHeight: 1.1,
  textTransform: "none",
  color: "#232f3e",   // Uses your main blue for the text/icons
  transition: "all 0.15s ease-in-out",
  
  // 1. HOVER STATE: Changes to a distinct, solid light-grey with a soft shadow
  "&:hover": {
    backgroundColor: "#e0e0e0", // Solid, visible greyish color on hover
    color: "#232f3e",
    boxShadow: "0 2px 5px rgba(0,0,0,0.15)", // Noticeable shadow to make it stand out
  },

  // 2. ON-CLICK STATE (Mouse pressing down): Deepens to a darker grey
  "&:active": {
    backgroundColor: "#85888b", // Darker grey on click
    boxShadow: "inset 0 2px 4px rgba(0,0,0,0.2)", // Inset shadow makes it look physically pressed
  },

  // 3. SELECTED STATE: Turns your main blue color when active/selected
  "&.Mui-selected": {
    backgroundColor: "#85888b", 
    color: "#ffffff", // Inverts text to white so it's highly visible
    boxShadow: "inset 0 3px 5px rgba(0,0,0,0.3)", // Heavy inset shadow for that "clicked in" look
    
    "&:hover": {
      backgroundColor: "#85888b", // Even darker brand blue if they hover while selected
    },
  },
};
*/

export default WorldCupFixturesCard;
