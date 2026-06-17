import {
  Box,
  Button,
  CircularProgress,
  Modal,
  TextField,
  Typography,
} from "@mui/material";
import React, { useContext, useState } from "react";
import { UserContext } from "../Context/UserContext";
import DoneOutlinedIcon from "@mui/icons-material/DoneOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import "../App.css";
import axios from "axios";
import { apiBaseUrl } from "../config/Config";

const style = {
  position: "absolute",
  top: "50%",
  left: "50%",
  transform: "translate(-50%, -50%)",
  width: "min(420px, 90vw)",
  bgcolor: "background.paper",
  border: "2px solid #000",
  borderRadius: "12px",
  p: 4,
};
const authTypeStyle = {
  width: "100px",
  color: "grey",
  fontSize: "20px",
};

const highlightedAuthTypeStyle = {
  width: "100px",
  color: "black",
  fontSize: "20px",
  fontWeight: "bold",
};

const AuthModal = ({
  authModalOpen,
  setAuthModalOpen,
  authModalTypes,
  authModalType,
  setAuthModalType,
}) => {
  const { setUserData } = useContext(UserContext);
  const [user, setUser] = useState({ username: "", password: "" });
  const [authNotification, setAuthNotification] = useState(null);
  const [authReqLoading, setAuthReqLoading] = useState(false);
  const [submitBtnDisabled, setSubmitBtnDisabled] = useState(false);

  const handleChange = (e) => {
    const name = e.target.name;
    const value = e.target.value;

    setUser({ ...user, [name]: value });
  };

  const handleAuthSuccess = (userApiRes) => {
    let msg = "";
    if (authModalType === authModalTypes.SIGNUP) {
      msg = "Account created successfully";
      setAuthNotification({ type: "success", msg });

      setTimeout(() => {
        resetModalStates();
        setAuthState(userApiRes);
      }, 2000);
    } else {
      resetModalStates();
      setAuthState(userApiRes);
    }
  };

  const setAuthState = (userApiRes) => {
    setUserData(userApiRes);
    localStorage.setItem("auth-token", userApiRes.token);
  };

  const resetModalStates = () => {
    setSubmitBtnDisabled(false);
    setAuthNotification(null);
    setAuthModalOpen(false);
    setAuthModalType(null);
  };

  const handleAuthError = (msg) => {
    setAuthNotification({ type: "fail", msg });

    setTimeout(() => {
      // resetModalStates();
      setSubmitBtnDisabled(false);
    }, 2000);
  };

  const handleAuthSubmit = async (event) => {
    event.preventDefault();
    setAuthReqLoading(true);
    setSubmitBtnDisabled(true);
    let baseUrl = `${apiBaseUrl}/auth`;
    let url;
    if (authModalType === authModalTypes.SIGNUP) {
      url = baseUrl + "/register";
    } else if (authModalType === authModalTypes.SIGNIN) {
      url = baseUrl + "/login";
    } else return;
    try {
      const authRes = await axios.post(url, user);
      setAuthReqLoading(false);
      if (!authRes || !authRes.data) {
        handleAuthError("error please try again later");
      }
      if (authRes.data.status != 0) {
        handleAuthError(authRes.data.errMsg);
      }
      handleAuthSuccess({ user: authRes.data.user, token: authRes.data.token });
    } catch (error) {
      console.log(error);
      handleAuthError("wrong username or password");
      setAuthReqLoading(false);
    }
  };

  return (
    <div>
      <Modal
        open={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <Box sx={style}>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-around",
              // backgroundColor: "red",
              height: "100%",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: "20px",
                gap: "50px",
              }}
            >
              <Button
                style={
                  authModalType === authModalTypes.SIGNIN
                    ? highlightedAuthTypeStyle
                    : authTypeStyle
                }
                size="small"
                onClick={() => setAuthModalType(authModalTypes.SIGNIN)}
              >
                Log in
              </Button>
              <Button
                style={
                  authModalType === authModalTypes.SIGNUP
                    ? highlightedAuthTypeStyle
                    : authTypeStyle
                }
                size="small"
                onClick={() => setAuthModalType(authModalTypes.SIGNUP)}
              >
                Sign Up
              </Button>
            </div>
            <Typography id="modal-modal-title" variant="h5" component="h2">
              {authModalType === authModalTypes.SIGNIN ? "Sign in" : "Sign Up"}
            </Typography>
            <form
              noValidate
              style={{ marginTop: "20px" }}
              onSubmit={handleAuthSubmit}
            >
              <TextField
                variant="outlined"
                margin="normal"
                value={user.username}
                onChange={handleChange}
                fullWidth
                id="username"
                label="Username"
                name="username"
                autoComplete="username"
                autoFocus
              />
              <TextField
                variant="outlined"
                margin="normal"
                value={user.password}
                onChange={handleChange}
                fullWidth
                name="password"
                label="Password"
                type="password"
                autoComplete="current-password"
              />
              <div className="flex-left" style={{ marginTop: "20px" }}>
                <Button
                  type="submit"
                  style={{ width: "100px" }}
                  variant="contained"
                  size="small"
                  disabled={submitBtnDisabled}
                >
                  {authReqLoading ? (
                    <CircularProgress size="1.4rem" />
                  ) : authModalType === authModalTypes.SIGNIN ? (
                    "Login"
                  ) : (
                    "Register"
                  )}
                </Button>
                {authNotification && (
                  <div className="flex-left" style={{ marginLeft: "20px" }}>
                    {authNotification.type === "success" ? (
                      <DoneOutlinedIcon
                        style={{
                          backgroundColor: "green",
                          borderRadius: "50%",
                          marginRight: "8px",
                          marginLeft: "5px",
                        }}
                      />
                    ) : (
                      <CloseOutlinedIcon
                        style={{
                          backgroundColor: "red",
                          borderRadius: "50%",
                          marginRight: "8px",
                          marginLeft: "5px",
                        }}
                      />
                    )}

                    <div>{authNotification.msg}</div>
                  </div>
                )}
              </div>
            </form>
          </div>
        </Box>
      </Modal>
    </div>
  );
};

export default AuthModal;
