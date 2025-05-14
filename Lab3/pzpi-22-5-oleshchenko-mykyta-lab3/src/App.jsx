import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import UserPanel from './pages/UserPanel'
import AdminPanel from './pages/AdminPanel'
import ProtectedRoute from './components/ProtectedRoute'
import UserNotes from './pages/UserNotes'

function App() {
  return (
    <Routes>
      {/* перенаправлення головної сторінки на логін */}
      <Route path="/" element={<Navigate to="/login" />} />

      {/* логін */}
      <Route path="/login" element={<Login />} />

      {/* клієнт */}
      <Route
        path="/user"
        element={
          <ProtectedRoute allowedRole="client">
            <UserPanel />
          </ProtectedRoute>
        }
      />

      {/* адміністратор */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRole="admin">
            <AdminPanel />
          </ProtectedRoute>
        }
      />

      <Route
        path="/user-notes"
        element={
          <ProtectedRoute allowedRole="client">
            <UserNotes />
          </ProtectedRoute>
        }
      />

      
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRole="admin">
            <AdminPanel />
          </ProtectedRoute>
        }
      />
      
    </Routes>
  )
}

export default App
