// AdminPanel.jsx
import { useEffect, useState } from 'react'
import axios from 'axios'
import { useTranslation } from 'react-i18next'
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
import './AdminPanel.css'

function AdminPanel() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [accounts, setAccounts] = useState([])
  const [filteredAccounts, setFilteredAccounts] = useState([])
  const [selectedNotes, setSelectedNotes] = useState([])
  const [selectedResults, setSelectedResults] = useState([])
  const [selectedEmail, setSelectedEmail] = useState(null)
  const [editingNoteId, setEditingNoteId] = useState(null)
  const [editedText, setEditedText] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchAccounts()
  }, [])

  useEffect(() => {
    const filtered = accounts.filter((acc) =>
      acc[1].toLowerCase().includes(searchTerm.toLowerCase()) ||
      acc[2].toLowerCase().includes(searchTerm.toLowerCase())
    )
    setFilteredAccounts(filtered)
  }, [searchTerm, accounts])

  const fetchAccounts = async () => {
    const role = localStorage.getItem('role')
    try {
      const response = await axios.get('http://localhost:5000/accounts', {
        headers: { Role: role }
      })
      setAccounts(response.data)
      setFilteredAccounts(response.data)
    } catch (err) {
      console.error('Error fetching accounts:', err)
    }
  }

  const fetchNotes = async (email) => {
    const role = localStorage.getItem('role')
    try {
      const response = await axios.get('http://localhost:5000/accounts/notes', {
        params: { email },
        headers: { Role: role }
      })
      setSelectedEmail(email)
      setSelectedNotes(response.data)
      setSelectedResults([])
    } catch (err) {
      console.error('Error fetching notes:', err)
    }
  }

  const fetchResults = async (email) => {
    const role = localStorage.getItem('role')
    try {
      const response = await axios.get('http://localhost:5000/user-results', {
        params: { email },
        headers: { Role: role }
      })
      setSelectedEmail(email)
      setSelectedResults(response.data)
      setSelectedNotes([])
    } catch (err) {
      console.error('Error fetching results:', err)
    }
  }

  const deleteAccount = async (email) => {
    const role = localStorage.getItem('role')
    if (!window.confirm(`Are you sure you want to delete ${email}?`)) return

    try {
      await axios.delete('http://localhost:5000/accounts', {
        params: { email },
        headers: { Role: role }
      })
      fetchAccounts()
      setSelectedNotes([])
      setSelectedResults([])
      setSelectedEmail(null)
    } catch (err) {
      console.error('Error deleting account:', err)
      alert('Failed to delete account')
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('email')
    localStorage.removeItem('role')
    navigate('/login')
  }

  const handleNoteEdit = async (noteID) => {
    const role = localStorage.getItem('role')
    try {
      await axios.put(`http://localhost:5000/notes/${noteID}`, {
        text: editedText
      }, {
        headers: { Role: role }
      })
      setEditingNoteId(null)
      setEditedText('')
      fetchNotes(selectedEmail)
    } catch (err) {
      console.error('Error updating note:', err)
    }
  }

  const downloadBackup = async () => {
    const role = localStorage.getItem('role')
    try {
      const response = await axios.get('http://localhost:5000/backup', {
        headers: { Role: role },
        responseType: 'blob'
      })
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'backup.json')
      document.body.appendChild(link)
      link.click()
    } catch (err) {
      console.error('Backup failed:', err)
    }
  }

  const uploadBackup = async (event) => {
    const file = event.target.files[0]
    const role = localStorage.getItem('role')
    if (!file) return

    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const jsonData = JSON.parse(e.target.result)
        await axios.post('http://localhost:5000/restore', jsonData, {
          headers: { 'Content-Type': 'application/json', Role: role }
        })
        fetchAccounts()
        if (selectedEmail) fetchNotes(selectedEmail)
        alert('Імпорт завершено')
      } catch (err) {
        console.error('Import error:', err)
        alert('Помилка імпорту')
      }
    }
    reader.readAsText(file)
  }

  return (
    <div className="admin-wrapper">
      <div className="admin-controls-top">
        <button onClick={() => i18n.changeLanguage('uk')}>UA</button>
        <button onClick={() => i18n.changeLanguage('en')}>EN</button>
        <button onClick={handleLogout}>🔓 {t('logout')}</button>
        <button onClick={downloadBackup}>📥 Download backup</button>
        <button onClick={() => document.getElementById('uploadInput').click()}>📤 Upload backup</button>
        <input
          type="file"
          id="uploadInput"
          accept="application/json"
          onChange={uploadBackup}
          style={{ display: 'none' }}
        />
      </div>

      <div className="admin-box">
        <h2>{t('admin_panel')}</h2>

        <input
          type="text"
          placeholder={t('search')}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ marginBottom: '15px', padding: '8px', width: '100%' }}
        />

        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>{t('email')}</th>
              <th>{t('name')}</th>
              <th>{t('role')}</th>
              <th>{t('actions')}</th>
            </tr>
          </thead>
          <tbody>
            {filteredAccounts.map((acc) => (
              <tr key={acc[0]}>
                <td>{acc[0]}</td>
                <td>{acc[1]}</td>
                <td>{acc[2]}</td>
                <td>{acc[3]}</td>
                <td>
                  <button onClick={() => fetchNotes(acc[1])}>📒 {t('notes')}</button>
                  <button onClick={() => fetchResults(acc[1])}>📊 {t('results')}</button>
                  {acc[3] === 'client' && (
                    <button onClick={() => deleteAccount(acc[1])}>❌ {t('delete')}</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {selectedNotes.length > 0 && (
          <div className="notes-box">
            <h3>{t('notes_for')} {selectedEmail}</h3>
            <ul>
              {selectedNotes.map((note) => (
                <li key={note.NoteID}>
                  <strong>{new Date(note.CreationDate).toLocaleString(i18n.language)}</strong>:
                  {editingNoteId === note.NoteID ? (
                    <>
                      <input
                        type="text"
                        value={editedText}
                        onChange={(e) => setEditedText(e.target.value)}
                      />
                      <button onClick={() => handleNoteEdit(note.NoteID)}>💾</button>
                    </>
                  ) : (
                    <>
                      {' '}{note.Text}{' '}
                      <button onClick={() => { setEditingNoteId(note.NoteID); setEditedText(note.Text) }}>✏️</button>
                    </>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        {selectedResults.length > 0 && (
          <div className="results-box">
            <h3>{t('results_for')} {selectedEmail}</h3>
            <table className="results-table">
              <thead>
                <tr>
                  <th>{t('date')}</th>
                  <th>{t('state')}</th>
                  <th>{t('stress')}</th>
                </tr>
              </thead>
              <tbody>
                {selectedResults.map((r, index) => (
                  <tr key={index}>
                    <td>{new Date(r.date).toLocaleString(i18n.language)}</td>
                    <td>{r.state}</td>
                    <td>{r.stress}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="chart-box">
              <h3>{t('stress_chart')}</h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={selectedResults.slice().reverse()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={(tick) => new Date(tick).toLocaleDateString(i18n.language)} />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="stress" stroke="#8884d8" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminPanel
