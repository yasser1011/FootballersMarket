import React, { useState } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import UserRankingRow from "./UserRankingRow";
import "./UserRankings.css";
import "../../App.css";
import { Button, CardActions, CircularProgress } from "@mui/material";
import axios from "axios";
import { useOnMountUnsafe } from "../../customhooks/useOnMountUnsafe";
import { useNavigate } from "react-router-dom";
import { apiBaseUrl } from "../../config/Config";

const UsersRanking = () => {
  const FETCH_STATUS = {
    ERROR: "error",
    SUCCESS: "success",
    LOADING: "loading",
  };

  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [fetchStatus, setFetchStatus] = useState(FETCH_STATUS.SUCCESS);

  const fetchUsers = async () => {
    try {
      let usersRes = await axios.get(`${apiBaseUrl}/users/top-users`);
      if (usersRes && usersRes.data) {
        setUsers(usersRes.data);
        setFetchStatus(FETCH_STATUS.SUCCESS);
      }
    } catch (error) {
      setFetchStatus(FETCH_STATUS.ERROR);
    }
  };

  const navigateToUsersRanking = () => {
    return navigate(`/rankings`, {});
  };

  useOnMountUnsafe(() => {
    fetchUsers();
  }, []);

  if (fetchStatus === FETCH_STATUS.LOADING) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <div
            style={{
              height: "100%",
            }}
            className="center-div"
          >
            <CircularProgress />
          </div>
        </Card>
      </Box>
    );
  }

  if (fetchStatus === FETCH_STATUS.ERROR) {
    return (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <h4>Error Please try again later</h4>
        </Card>
      </Box>
    );
  }

  return (
    users && (
      <Box sx={{ minWidth: 275 }}>
        <Card
          style={{ borderRadius: "10px", width: "25rem", height: "18rem" }}
          variant="outlined"
        >
          <div className="title-style">Top Users</div>
          <div style={{ height: "73%" }}>
            <table className="top-users-table">
              <thead className="top-users-table-header">
                <tr style={{ backgroundColor: "rgba(229, 233, 239, 0.4)" }}>
                  <th className="top-users-table-header-th">
                    <div className="top-users-table-header-username">
                      Username
                    </div>
                  </th>
                  <th className="top-users-table-header-th">
                    <div className="top-users-table-header-score">Score</div>
                  </th>
                </tr>
              </thead>

              <tbody>
                {users.map((user, idx) => {
                  return <UserRankingRow key={idx} user={user} id={idx + 1} />;
                })}
              </tbody>
            </table>
          </div>
          <CardActions>
            <Button
              variant="contained"
              size="small"
              onClick={navigateToUsersRanking}
            >
              Show More
            </Button>
          </CardActions>
        </Card>
      </Box>
    )
  );
};

export default UsersRanking;
