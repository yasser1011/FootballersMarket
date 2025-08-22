import React from "react";
import "./UserRankings.css";

const UserRankingRow = ({ user, id }) => {
  return (
    <tr style={{ backgroundColor: "white" }}>
      <td style={{ padding: "0", border: "1px solid rgba(34, 34, 38, 0.15)" }}>
        <div style={{ display: "flex", height: "32px" }}>
          <div className="top-users-row">
            <div>
              <span className="top-user-rank-number">{id}- </span>
            </div>
            <div>
              <span className="top-user-rank-username">{user.username}</span>
            </div>
          </div>
        </div>
      </td>
      <td style={{ padding: "0", border: "1px solid rgba(34, 34, 38, 0.15)" }}>
        <div className="top-user-rank-score-wrapper">
          <div className="top-user-rank-score-number">{user.points}</div>
        </div>
      </td>
    </tr>
  );
};

{
  /* <div>
        <span className="top-user-data">{id}- </span>
        <span className="top-user-data">{user.username}</span>
      </div>
      <div>
        <span className="top-user-data">{user.points}</span>
      </div> */
}

export default UserRankingRow;
