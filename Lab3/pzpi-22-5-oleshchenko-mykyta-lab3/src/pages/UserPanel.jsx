import { useEffect, useState } from 'react'
import axios from 'axios'
import { useTranslation } from 'react-i18next'
import './UserPanel.css'
import { useNavigate } from 'react-router-dom'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts'

function UserPanel() {
  const { t, i18n } = useTranslation()
  const [userData, setUserData] = useState(null)
  const [error, setError] = useState('')
  const [results, setResults] = useState([])
  const [newNote, setNewNote] = useState('')
  const [editMode, setEditMode] = useState(false)
  const [editedNote, setEditedNote] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const email = localStorage.getItem('email')
        const role = localStorage.getItem('role')

        const response = await axios.get('http://localhost:5000/user-info', {
          params: { email },
          headers: { Role: role }
        })

        setUserData(response.data)
        setEditedNote(response.data.note || '')
      } catch (err) {
        console.error('Error fetching user data:', err)
        setError(t('error_loading'))
      }
    }

    const fetchResults = async () => {
      try {
        const email = localStorage.getItem('email')
        const role = localStorage.getItem('role')

        const response = await axios.get('http://localhost:5000/user-results', {
          params: { email },
          headers: { Role: role }
        })

        setResults(response.data)
      } catch (err) {
        console.error('Error fetching results:', err)
      }
    }

    fetchUserData()
    fetchResults()
  }, [t])

  const handleSaveNote = async () => {
    try {
      const email = localStorage.getItem('email')
      const role = localStorage.getItem('role')

      await axios.put('http://localhost:5000/notes',
        { text: editedNote },
        {
          params: { email },
          headers: { Role: role }
        }
      )

      setUserData(prev => ({ ...prev, note: editedNote }))
      setEditMode(false)
    } catch (err) {
      console.error('Error updating note:', err)
      alert(t('error_updating_note'))
    }
  }

  const handleAddNote = async () => {
    try {
      const email = localStorage.getItem('email')
      const role = localStorage.getItem('role')
  
      await axios.post('http://localhost:5000/notes',
        { text: newNote },
        {
          headers: {
            Role: role,
            Email: email
          }
        }
      )
  
      alert(t('note_added'))
      setNewNote('')
      setUserData(prev => ({ ...prev, note: newNote }))
    } catch (err) {
      console.error('Error adding note:', err)
      alert(t('error_adding_note'))
    }
  }
  
  const handleLogout = () => {
    localStorage.removeItem('email')
    localStorage.removeItem('role')
    navigate('/login')
  }

  if (error) return <p style={{ color: 'red' }}>{error}</p>
  if (!userData) return <p>{t('loading')}...</p>

  return (
    <div className="user-wrapper">

        <div className="top-buttons">
        <button onClick={() => navigate('/user-notes')}>
         {t('notes_history')}
          </button>
          <button onClick={handleLogout}>
            {t('logout')}
          </button>
        </div>


      <div className="user-box">
        <h2>{t('user_panel')}</h2>


        <p><strong>{t('email')}:</strong> {userData.email}</p>
        <p><strong>{t('name')}:</strong> {userData.name}</p>
        <p><strong>{t('emotional_state')}:</strong> {userData.state}</p>

        <p><strong>{t('note')}:</strong></p>
        
        {!editMode ? (
          <>
            <p>{userData.note}</p>
            <button onClick={() => setEditMode(true)}>✏️ {t('edit')}</button>
          </>
        ) : (
          <>
            <textarea
              value={editedNote}
              onChange={(e) => setEditedNote(e.target.value)}
              rows={4}
              cols={40}
            />
            <br />
            <button onClick={handleSaveNote}>{t('save')}</button>
            <button onClick={() => setEditMode(false)}>{t('cancel')}</button>
          </>
        )}

        <div style={{ marginTop: '30px' }}>
            <h4>{t('add_new_note')}</h4>
            <textarea
            value={newNote}
            onChange={(e) => setNewNote(e.target.value)}
            rows={3}
            cols={40}
          />
          <br />
          <button onClick={handleAddNote}>{t('add')}</button>
        </div>

        <div>
          <p>{t('change_language')}:</p>
          <button onClick={() => i18n.changeLanguage('uk')}>UA</button>
          <button onClick={() => i18n.changeLanguage('en')}>EN</button>
        </div>

        {results.length > 0 && (
          <div className="results-and-chart">
            <div className="results-box">
              <h3>{t('result_history')}</h3>
              <table className="results-table">
                <thead>
                  <tr>
                    <th>{t('date')}</th>
                    <th>{t('state')}</th>
                    <th>{t('stress')}</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((r, index) => (
                    <tr key={index}>
                      <td>{new Date(r.date).toLocaleString()}</td>
                      <td>{r.state}</td>
                      <td>{r.stress}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="chart-box">
              <h3>{t('stress_chart')}</h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={results.slice().reverse()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="date"
                    tickFormatter={(tick) => new Date(tick).toLocaleDateString()}
                  />
                  <YAxis />
                  <Tooltip />
                  <Line
                    type="monotone"
                    dataKey="stress"
                    stroke="#8884d8"
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default UserPanel
