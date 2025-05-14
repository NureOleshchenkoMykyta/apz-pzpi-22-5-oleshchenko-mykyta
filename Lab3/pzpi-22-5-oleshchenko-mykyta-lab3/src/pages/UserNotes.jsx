import { useEffect, useState } from 'react'
import axios from 'axios'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import './UserNotes.css'

function UserNotes() {
  const { t } = useTranslation()
  const [notes, setNotes] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    const fetchNotes = async () => {
      try {
        const email = localStorage.getItem('email')
        const role = localStorage.getItem('role')

        const response = await axios.get('http://localhost:5000/accounts/notes', {
          params: { email },
          headers: { Role: role }
        })

        setNotes(response.data)
      } catch (err) {
        console.error('Error fetching notes:', err)
      }
    }

    fetchNotes()
  }, [])

  return (
    <div className="notes-wrapper">
      <div className="notes-box">
        <h2>{t('notes_history')}</h2>
        <button onClick={() => navigate('/user')}>
          {t('back_to_panel')}
        </button>

        <table className="notes-table">
          <thead>
            <tr>
              <th>{t('date')}</th>
              <th>{t('text')}</th>
            </tr>
          </thead>
          <tbody>
            {notes.map(note => (
              <tr key={note.NoteID}>
                <td>{new Date(note.CreationDate).toLocaleString()}</td>
                <td>{note.Text}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default UserNotes
