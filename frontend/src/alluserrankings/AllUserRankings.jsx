import React, { useState } from "react";
import "../App.css";
import "./PredictionHistory.css";
import {
  Paper,
  Table,
  TableContainer,
  TableHead,
  TableRow,
} from "@mui/material";
import { useSearchParams } from "react-router-dom";
import { StyledTableCell } from "../home/Home";
import UserRankingTableBody from "./UserRankingTableBody";
import UserPredictionHistory from "./UserPredictionHistory";

const AllUserRankings = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  // selection can arrive via ?userId= (e.g. clicking a user in the Top Users home card)
  const [selected, setSelected] = useState({
    userId: searchParams.get("userId") || "",
    username: "",
  });

  const onSelectUser = (user) => {
    setSelected({ userId: String(user.userId), username: user.username });
    setSearchParams({ userId: String(user.userId) }); // shareable + survives refresh
  };

  return (
    <div className="rankings-page">
      <div className="rankings-list">
        <TableContainer style={{ minHeight: "10rem" }} component={Paper}>
          <Table>
            <colgroup>
              <col style={{ width: "10%" }} />
              <col />
              <col />
            </colgroup>
            <TableHead>
              <TableRow>
                <StyledTableCell># </StyledTableCell>
                <StyledTableCell>User</StyledTableCell>
                <StyledTableCell>Score</StyledTableCell>
              </TableRow>
            </TableHead>
            <UserRankingTableBody
              selectedUserId={selected.userId}
              onSelectUser={onSelectUser}
            />
          </Table>
        </TableContainer>
      </div>

      <div className="rankings-history">
        <UserPredictionHistory
          userId={selected.userId}
          username={selected.username}
        />
      </div>
    </div>
  );
};

export default AllUserRankings;
