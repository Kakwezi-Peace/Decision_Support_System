import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute, AdminRoute } from "./ProtectedRoute";
import { Layout } from "./Layout";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { OperationalDataPage } from "./pages/OperationalDataPage";
import { DelayEventsPage } from "./pages/DelayEventsPage";
import { RecoveryPage } from "./pages/RecoveryPage";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { RecoveryHistoryPage } from "./pages/RecoveryHistoryPage";
import { UsersPage } from "./pages/UsersPage";
import { ProfilePage } from "./pages/ProfilePage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/delay-events" element={<DelayEventsPage />} />
              <Route path="/delay-events/:delayEventId" element={<RecoveryPage />} />
              <Route path="/operations" element={<OperationalDataPage />} />
              <Route path="/analytics" element={<AnalyticsPage />} />
              <Route path="/history" element={<RecoveryHistoryPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route element={<AdminRoute />}>
                <Route path="/users" element={<UsersPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
