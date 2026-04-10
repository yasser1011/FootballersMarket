import * as React from "react";
import "./Home.css";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import { styled, withStyles } from "@mui/material/styles";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import MuiTableCell from "@mui/material/TableCell";
import Paper from "@mui/material/Paper";
import { Button, CircularProgress, TableCell } from "@mui/material";
import { useState } from "react";
import { StyledTableCell } from "./Home";
import { BorderBottom } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { useEffect } from "react";
import { useOnMountUnsafe } from "../customhooks/useOnMountUnsafe";
import { UserContext } from "../Context/UserContext";
import { useContext } from "react";

const StyledTableRow = styled(TableRow)(({ theme }) => ({
  // "&:nth-of-type(odd)": {
  //   backgroundColor: theme.palette.action.hover,
  // },
  // hide last border
  "&:last-child td, &:last-child th": {
    border: 0,
  },
}));

export const getAge = (birthDate) =>
  Math.floor((new Date() - new Date(birthDate).getTime()) / 3.15576e10);

export default function HomeTable({
  fetchPlayersUrl,
  setPaginationOptions,
  paginationOptions,
}) {
  const { userData } = useContext(UserContext);

  let ranOnce = false;

  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const [players, setPlayers] = useState([]);
  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.LOADING);

  const navigate = useNavigate();

  const fetchPlayers = async () => {
    setFetchStatus(FETCH_STATUS.LOADING);
    // add token if the url includes /myteam
    let playersRes;
    try {
      if (fetchPlayersUrl.includes("myteam")) {
        if (!userData.user) return;
        playersRes = await axios.get(fetchPlayersUrl, {
          headers: { Authorization: userData.token },
        });
      } else {
        playersRes = await axios.get(fetchPlayersUrl);
      }

      if (playersRes && playersRes.data) {
        if (fetchPlayersUrl.includes("myteam")) {
          setPlayers(playersRes.data);
        } else {
          setPlayers(playersRes.data.content);
          setPaginationOptions((currOptions) => {
            return {
              ...currOptions,
              disabled: false,
              rowsPerPage: 50,
              totalCount: playersRes.data.totalElements,
            };
          });
        }
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  const navigatePlayerDetails = (player) => {
    // return navigate(`/players/rp/${player.id}`, {
    //   state: { player, srcLocation: "hometable" },
    // });
    return navigate(`/players/${player.externalServicePlayerId}`, {
      state: { player },
    });
  };

  useEffect(() => {
    if (!ranOnce) {
      fetchPlayers();
      ranOnce = true;
    }
  }, [fetchPlayersUrl]);

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <>
        <TableBody>
          <StyledTableRow>
            <TableCell align="center" colSpan={10}>
              <CircularProgress />
            </TableCell>
          </StyledTableRow>
        </TableBody>
      </>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <>
        <TableBody>
          <StyledTableRow>
            <TableCell align="center" colSpan={10}>
              Error Please Try Again Later
            </TableCell>
          </StyledTableRow>
        </TableBody>
      </>
    );
  }
  return (
    players && (
      <TableBody className="main-table-body">
        {players.map((player, idx) => {
          return (
            <StyledTableRow
              onClick={() => navigatePlayerDetails(player)}
              style={{ cursor: "pointer" }}
              key={player.id}
            >
              <StyledTableCell component="th" scope="row">
                <div className="table-player-col-style">
                  <span className="table-player-col-number-style">
                    {paginationOptions.rowsPerPage *
                      paginationOptions.currentPage +
                      (idx + 1)}{" "}
                  </span>
                  <img
                    src={player.photoUrl}
                    width={40}
                    height={40}
                    style={{ borderRadius: "50%" }}
                    alt=""
                  />
                  <span className="table-player-col-name-style">
                    {player.name}
                  </span>
                </div>
              </StyledTableCell>
              <StyledTableCell
              // className="table-player-col-name-style"
              // align="center"
              >
                <div className="table-player-col-style">
                  <img
                    src={player.clubPhotoUrl}
                    width={40}
                    height={40}
                    style={{ borderRadius: "30%" }}
                    alt=""
                  />
                  <span className="table-player-col-name-style">
                    {player.externalServicePlayerClub}
                  </span>
                </div>
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.nationality}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {getAge(player.dateOfBirth)}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.position}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.avgRating}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.totalGoals}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.totalAssists}
              </StyledTableCell>
              {fetchPlayersUrl.includes("myteam") && (
                <StyledTableCell
                  className="table-player-col-name-style"
                  align="center"
                >
                  {player.buyPrice}
                </StyledTableCell>
              )}
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                {player.price}
              </StyledTableCell>
              <StyledTableCell
                className="table-player-col-name-style"
                align="center"
              >
                <Button size="small" variant="contained">
                  Details
                </Button>
              </StyledTableCell>
            </StyledTableRow>
          );
        })}
      </TableBody>
    )
  );
}
