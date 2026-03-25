import React from "react";
import "../App.css";
import {
  Paper,
  Table,
  TableContainer,
  TableHead,
  TableRow,
} from "@mui/material";
import { StyledTableCell } from "../home/Home";
import HomeTable from "../home/HomeTable";
import { UserContext } from "../Context/UserContext";
import { useContext } from "react";
import { apiBaseUrl } from "../config/Config";

const MyTeam = () => {
  const { userData } = useContext(UserContext);

  if (!userData.user) {
    return (
      <div className="center-div">
        <h1>Please Login to view your team</h1>
      </div>
    );
  }

  return (
    <div className="center-div">
      <div
        style={{
          width: "90%",
          //   border: "solid",
          //   height: "200px",
          margin: "20px",
          borderRadius: "10px",
        }}
      >
        <TableContainer style={{ minHeight: "10rem" }} component={Paper}>
          <Table sx={{ minWidth: 700 }} aria-label="customized table">
            <colgroup>
              <col style={{ width: "20%" }} />
              <col style={{ width: "15%" }} />
              <col style={{ width: "10%" }} />
              <col style={{ width: "10%" }} />
              <col style={{ width: "10%" }} />
              <col style={{ width: "5%" }} />
              <col style={{ width: "5%" }} />
              <col style={{ width: "5%" }} />
              <col style={{ width: "10%" }} />
              <col style={{ width: "10%" }} />
            </colgroup>
            <TableHead>
              <TableRow>
                <StyledTableCell># Player</StyledTableCell>
                <StyledTableCell align="center">Club</StyledTableCell>
                <StyledTableCell align="center">Nationality</StyledTableCell>
                <StyledTableCell align="center">Age</StyledTableCell>
                <StyledTableCell align="center">Position</StyledTableCell>
                <StyledTableCell align="center">Rating</StyledTableCell>
                <StyledTableCell align="center">Goals</StyledTableCell>
                <StyledTableCell align="center">Asissts</StyledTableCell>
                <StyledTableCell align="center">Buy Price</StyledTableCell>
                <StyledTableCell align="center">Price</StyledTableCell>
                <StyledTableCell align="center"></StyledTableCell>
              </TableRow>
            </TableHead>
            <HomeTable fetchPlayersUrl={`${apiBaseUrl}/users/myteam`} />
          </Table>
        </TableContainer>
      </div>
    </div>
  );
};

export default MyTeam;
