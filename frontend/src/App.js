import "./App.css";
import Navbar from "./navbar/Navbar";
import Home from "./home/Home";
import Error from "./error/Error";
import PlayerDetails from "./playerdetails/PlayerDetails";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import MyTeam from "./myteam/MyTeam";
import AllUserRankings from "./alluserrankings/AllUserRankings";

function App() {
  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navbar />}>
            <Route index element={<Home />} />
            {/* world cup mode lives under the /worldcup url prefix (source of truth for WC mode);
                player details opened in WC mode is /worldcup/players/:id */}
            <Route path="worldcup">
              <Route index element={<Home worldCup={true} />} />
              <Route path="players/:id" element={<PlayerDetails />} />
              <Route path="rankings" element={<AllUserRankings />} />
              <Route path="myteam" element={<MyTeam />} />
            </Route>
            <Route path="players/:id" element={<PlayerDetails />} />
            <Route path="myteam" element={<MyTeam />} />
            <Route path="rankings" element={<AllUserRankings />} />
            <Route path="*" element={<Error />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
