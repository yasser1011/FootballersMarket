import React from "react";
import "./UserRankings.css";

const UserRankingRow = ({ user, id, onClick }) => {
  return (
    <tr className="top-users-data-row" onClick={onClick} style={{ cursor: "pointer" }}>
      <td style={{ padding: "0", borderBottom: "1px solid #e3e7ec" }}>
        <div style={{ display: "flex", height: "34px" }}>
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
      <td style={{ padding: "0", borderBottom: "1px solid #e3e7ec" }}>
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
