import React, { useState } from "react";
import "../App.css";
import {
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from "@mui/material";
import { StyledTableCell } from "../home/Home";
import UserRankingTableBody from "./UserRankingTableBody";

const AllUserRankings = () => {
  return (
    <div
      style={{
        width: "50%",
        margin: "20px",
        borderRadius: "10px",
      }}
    >
      <TableContainer style={{ minHeight: "10rem" }} component={Paper}>
        <Table>
          <colgroup>
            <col style={{ width: "5%" }} />
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
          <UserRankingTableBody />
        </Table>
      </TableContainer>
    </div>
  );
};

export default AllUserRankings;
