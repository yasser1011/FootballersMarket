import React, { useContext, useEffect, useState } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import { Button, CircularProgress, Typography } from "@mui/material";
import GroupsIcon from "@mui/icons-material/Groups";
import ArrowDropUpIcon from "@mui/icons-material/ArrowDropUp";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import { useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import { UserContext } from "../../Context/UserContext";
import { SelectedHomeTablePlayerContext } from "../../Context/SelectedHomeTablePlayerContext";
import AuthModal from "../../auth/AuthModal";
import { apiBaseUrl } from "../../config/Config";
import "./MySquad.css";

const authModalTypes = { SIGNIN: "SIGNIN", SIGNUP: "SIGNUP" };
const cardStyle = { borderRadius: "10px", width: "25rem", height: "18rem" };

const MySquad = () => {
  const { userData } = useContext(UserContext);
  const { setSelectedHomeTablePlayer } = useContext(SelectedHomeTablePlayerContext);
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const navigateToPlayer = (p) => {
    setSelectedHomeTablePlayer(p);
    const prefix = pathname.startsWith("/worldcup") ? "/worldcup" : "";
    navigate(`${prefix}/players/${p.externalServicePlayerId}`);
  };

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalType, setAuthModalType] = useState(null);
  const [squad, setSquad] = useState([]);
  const [status, setStatus] = useState("idle"); // idle | loading | success | error

  const openSignin = () => {
    setAuthModalType(authModalTypes.SIGNIN);
    setAuthModalOpen(true);
  };

  useEffect(() => {
    if (!userData.user) {
      setSquad([]);
      return;
    }
    let active = true;
    setStatus("loading");
    axios
      .get(`${apiBaseUrl}/users/myteam`, {
        headers: { Authorization: userData.token },
      })
      .then((res) => {
        if (active) {
          setSquad(res.data || []);
          setStatus("success");
        }
      })
      .catch(() => active && setStatus("error"));
    return () => {
      active = false;
    };
  }, [userData.token, userData.user]);

  // WC rating when the player is a tournament participant, else his league rating
  const ratingOf = (p) =>
    p.worldCupStats?.rating != null
      ? p.worldCupStats.rating.toFixed(2)
      : (p.avgRating ?? "-");

  if (!userData.user) {
    return (
      <Box sx={{ minWidth: 275 }}>
        {authModalOpen && (
          <AuthModal
            authModalOpen={authModalOpen}
            setAuthModalOpen={setAuthModalOpen}
            authModalTypes={authModalTypes}
            authModalType={authModalType}
            setAuthModalType={setAuthModalType}
          />
        )}
        <Card style={cardStyle} variant="outlined">
          <div className="title-style">My Squad</div>
          <div className="mysquad-body">
            <GroupsIcon className="mysquad-icon" />
            <Typography className="mysquad-desc">
              Sign in to start building your Squad
            </Typography>
            <Button variant="contained" size="small" onClick={openSignin}>
              Sign In
            </Button>
          </div>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ minWidth: 275 }}>
      <Card style={cardStyle} variant="outlined">
        <div className="title-style">My Squad</div>
        <div className="mysquad-list-wrapper">
          {status === "loading" && (
            <div className="mysquad-center">
              <CircularProgress size="1.4rem" />
            </div>
          )}
          {status === "error" && (
            <div className="mysquad-empty">Couldn't load your squad</div>
          )}
          {status === "success" && squad.length === 0 && (
            <div className="mysquad-empty">
              No players yet — start building your squad.
            </div>
          )}
          {status === "success" && squad.length > 0 && (
            <div className="mysquad-list">
              {/* column header */}
              <div className="mysquad-row mysquad-row-head">
                <span className="mysquad-row-name">Player</span>
                <span className="mysquad-row-rating">Rtg</span>
                <span className="mysquad-row-prices">Buy → Now</span>
              </div>
              {squad.map((p) => {
                const up = p.price > p.buyPrice;
                const down = p.price < p.buyPrice;
                const cls = up ? "mysquad-up" : down ? "mysquad-down" : "";
                return (
                  <div key={p.id} className="mysquad-row" onClick={() => navigateToPlayer(p)} style={{ cursor: "pointer" }}>
                    <span className="mysquad-row-name">
                      <img
                        src={p.photoUrl}
                        width={22}
                        height={22}
                        className="mysquad-row-img"
                        alt=""
                      />
                      {p.name}
                    </span>
                    <span className="mysquad-row-rating">{ratingOf(p)}</span>
                    <span className="mysquad-row-prices">
                      <span className="mysquad-buy">{p.buyPrice}</span>
                      <span className="mysquad-arrow">→</span>
                      <span className={cls}>{p.price}</span>
                      {up && (
                        <ArrowDropUpIcon className="mysquad-up" fontSize="small" />
                      )}
                      {down && (
                        <ArrowDropDownIcon
                          className="mysquad-down"
                          fontSize="small"
                        />
                      )}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Card>
    </Box>
  );
};

export default MySquad;
