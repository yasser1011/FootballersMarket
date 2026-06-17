import React, { useEffect, useState } from "react";
import axios from "axios";
import { CircularProgress } from "@mui/material";
import { apiBaseUrl } from "../config/Config";
import { pointsBreakdown, signed } from "../worldcup/predictionPoints";
import "./PredictionHistory.css";

const FETCH = { IDLE: "idle", LOADING: "loading", SUCCESS: "success", ERROR: "error" };

// which side the user picked, by team name (or Draw)
const pickLabel = (item) => {
  if (item.predictedWinnerTeamId == null) return "Draw";
  return item.predictedWinnerTeamId === item.home?.id
    ? item.home?.name
    : item.away?.name;
};

// a user's finished-game prediction history (settled only) in a scrollable panel
const UserPredictionHistory = ({ userId, username }) => {
  const [status, setStatus] = useState(FETCH.IDLE);
  const [items, setItems] = useState([]);

  useEffect(() => {
    if (!userId) {
      setItems([]);
      setStatus(FETCH.IDLE);
      return;
    }
    let active = true;
    setStatus(FETCH.LOADING);
    axios
      .get(`${apiBaseUrl}/wc/predictions/user/${userId}`)
      .then((res) => {
        if (active) {
          setItems(res.data || []);
          setStatus(FETCH.SUCCESS);
        }
      })
      .catch(() => active && setStatus(FETCH.ERROR));
    return () => {
      active = false;
    };
  }, [userId]);

  if (!userId) {
    return (
      <div className="pred-history-empty">
        Select a user to see their prediction history
      </div>
    );
  }
  if (status === FETCH.LOADING) {
    return (
      <div className="pred-history-center">
        <CircularProgress />
      </div>
    );
  }
  if (status === FETCH.ERROR) {
    return <div className="pred-history-empty">Couldn't load predictions</div>;
  }
  if (items.length === 0) {
    return (
      <div className="pred-history-empty">No finished-game predictions yet</div>
    );
  }

  return (
    <div className="pred-history">
      <div className="pred-history-title">
        {username ? `${username}'s predictions` : "Predictions"}
      </div>
      <div className="pred-history-list">
        {items.map((item) => {
          const bd = pointsBreakdown(item);
          const up = bd != null && bd.total >= 0;
          return (
            <div key={item.fixtureId} className="pred-history-row">
              <div className="pred-history-round">{item.round}</div>

              {/* actual fixture result */}
              <div className="pred-history-match">
                <span className="pred-history-team">
                  <img src={item.home?.logoUrl} width={20} height={20} alt="" />
                  {item.home?.name}
                </span>
                <span className="pred-history-score">
                  {item.homeGoals} - {item.awayGoals}
                </span>
                <span className="pred-history-team pred-history-team-right">
                  {item.away?.name}
                  <img src={item.away?.logoUrl} width={20} height={20} alt="" />
                </span>
              </div>

              {/* the user's prediction + points + badges */}
              <div className="pred-history-bottom">
                <span className="pred-history-pick">
                  Pick: <b>{pickLabel(item)}</b>
                  {item.predictedHomeGoals != null &&
                    ` (${item.predictedHomeGoals}-${item.predictedAwayGoals})`}
                </span>
                <span className="pred-history-points">
                  {bd == null ? (
                    <span className="pred-points-pending">—</span>
                  ) : bd.hasParts ? (
                    <>
                      <span className="pred-part">
                        <span className="pred-part-label">Result</span>
                        <span
                          className={
                            bd.result >= 0 ? "pred-points-up" : "pred-points-down"
                          }
                        >
                          {signed(bd.result)}
                        </span>
                      </span>
                      {bd.exact > 0 && (
                        <span className="pred-part">
                          <span className="pred-part-label">Exact</span>
                          <span className="pred-points-up">
                            {signed(bd.exact)}
                          </span>
                        </span>
                      )}
                      {bd.bonus > 0 && (
                        <span className="pred-part">
                          <span className="pred-part-label">Bonus</span>
                          <span className="pred-points-up">
                            {signed(bd.bonus)}
                          </span>
                        </span>
                      )}
                      <span className="pred-part pred-part-total">
                        <span className="pred-part-label">Total</span>
                        <span className={up ? "pred-points-up" : "pred-points-down"}>
                          {signed(bd.total)}
                        </span>
                      </span>
                    </>
                  ) : (
                    <span className={up ? "pred-points-up" : "pred-points-down"}>
                      {signed(bd.total)}
                    </span>
                  )}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default UserPredictionHistory;
