import React, { useContext, useRef } from "react";
import HomeTable from "./HomeTable";
import HomeHeaderCards from "../cardssection/HomeHeaderCards";
import { styled } from "@mui/material/styles";
import TableCell, { tableCellClasses } from "@mui/material/TableCell";
import FirstPageRoundedIcon from "@mui/icons-material/FirstPageRounded";
import LastPageRoundedIcon from "@mui/icons-material/LastPageRounded";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import {
  MenuItem,
  Paper,
  Select,
  Table,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
} from "@mui/material";
import "./Home.css";
import { useState, useEffect } from "react";
import axios from "axios";
import { apiBaseUrl } from "../config/Config";
import { HomePageContext } from "../Context/HomePageContext";
import WorldCupHeader from "../worldcup/WorldCupHeader";

export const StyledTableCell = styled(TableCell)(({ theme }) => ({
  [`&.${tableCellClasses.head}`]: {
    // backgroundColor: theme.palette.common.black,
    backgroundColor: "var(--app-primary)",
    color: theme.palette.common.white,
  },
  [`&.${tableCellClasses.body}`]: {
    fontSize: 14,
  },
}));

const Home = ({ worldCup = false }) => {
  const {
    paginationOptions,
    setPaginationOptions,
    wcNationalityTeamId,
    setWcNationalityTeamId,
  } = useContext(HomePageContext);

  // world cup nationality filter — team list is local (just dropdown data)
  const [teams, setTeams] = useState([]);

  // Track previous worldCup value so we only reset state on an actual MODE SWITCH,
  // not on every mount (which would wipe the page / filter when navigating back).
  const prevWorldCupRef = useRef(worldCup);
  useEffect(() => {
    if (prevWorldCupRef.current !== worldCup) {
      prevWorldCupRef.current = worldCup;
      setPaginationOptions((curr) => ({ ...curr, currentPage: 0 }));
      if (!worldCup) setWcNationalityTeamId("");
    }
  }, [worldCup, setPaginationOptions, setWcNationalityTeamId]);

  // load the national teams for the filter dropdown (WC mode only)
  useEffect(() => {
    if (!worldCup) return;
    axios
      .get(`${apiBaseUrl}/wc/teams`)
      .then((res) => setTeams(res.data || []))
      .catch(() => setTeams([]));
  }, [worldCup]);

  const handleChangePage = (event, newPage) => {
    setPaginationOptions((currOptions) => {
      return {
        ...currOptions,
        currentPage: newPage,
      };
    });
  };

  const handleNationalityChange = (event) => {
    setWcNationalityTeamId(event.target.value);
    // a new filter restarts paging from the first page
    setPaginationOptions((currOptions) => ({ ...currOptions, currentPage: 0 }));
  };
  return (
    <div>
      {worldCup && <WorldCupHeader />}
      <HomeHeaderCards worldCup={worldCup} />
      <div className="table-container">
        <div style={{ width: "100%" }}>
          <TableContainer style={{ minHeight: "10rem" }} component={Paper}>
            <Table sx={{ minWidth: 700 }} aria-label="customized table">
              <colgroup>
                <col style={{ width: "20%" }} />
                {/* club column is hidden in world cup mode */}
                {!worldCup && <col style={{ width: "15%" }} />}
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
                  {!worldCup && (
                    <StyledTableCell align="center">Club</StyledTableCell>
                  )}
                  <StyledTableCell align="center">
                    {worldCup ? (
                      <Select
                        value={wcNationalityTeamId}
                        onChange={handleNationalityChange}
                        displayEmpty
                        variant="standard"
                        renderValue={(val) => {
                          if (!val) return "Nationality";
                          const team = teams.find(
                            (t) => String(t.id) === String(val),
                          );
                          return team ? team.name : "Nationality";
                        }}
                        sx={{
                          color: "#fff",
                          fontSize: "0.78rem",
                          "& .MuiSelect-icon": { color: "#fff" },
                          "& .MuiSelect-select": { paddingRight: "20px" },
                          "&:before, &:after": { borderColor: "rgba(255,255,255,0.5)" },
                        }}
                      >
                        <MenuItem value="">
                          <em>All nationalities</em>
                        </MenuItem>
                        {teams.map((team) => (
                          <MenuItem key={team.id} value={String(team.id)}>
                            <span
                              style={{
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 8,
                              }}
                            >
                              <img
                                src={team.logoUrl}
                                width={16}
                                height={16}
                                alt=""
                              />
                              {team.name}
                            </span>
                          </MenuItem>
                        ))}
                      </Select>
                    ) : (
                      "Nationality"
                    )}
                  </StyledTableCell>
                  <StyledTableCell align="center">Age</StyledTableCell>
                  <StyledTableCell align="center">Position</StyledTableCell>
                  <StyledTableCell align="center">Rating</StyledTableCell>
                  <StyledTableCell align="center">Goals</StyledTableCell>
                  <StyledTableCell align="center">Asissts</StyledTableCell>
                  <StyledTableCell align="center">Price</StyledTableCell>
                  <StyledTableCell align="center"></StyledTableCell>
                </TableRow>
              </TableHead>
              <HomeTable
                fetchPlayersUrl={`${apiBaseUrl}/players?page=${
                  paginationOptions.currentPage
                }${worldCup ? "&worldCup=true" : ""}${
                  worldCup && wcNationalityTeamId ? `&teamId=${wcNationalityTeamId}` : ""
                }`}
                worldCup={worldCup}
                setPaginationOptions={setPaginationOptions}
                paginationOptions={paginationOptions}
              />
              <tfoot>
                <tr>
                  <TablePagination
                    disabled={paginationOptions.disabled}
                    style={{
                      display: "flex",
                      width: "290px",
                    }}
                    count={paginationOptions.totalCount}
                    page={paginationOptions.currentPage}
                    rowsPerPageOptions={[]}
                    rowsPerPage={paginationOptions.rowsPerPage}
                    showFirstButton
                    showLastButton
                    onPageChange={handleChangePage}
                  />
                </tr>
              </tfoot>
            </Table>
          </TableContainer>
        </div>
      </div>
    </div>
  );
};

export default Home;
