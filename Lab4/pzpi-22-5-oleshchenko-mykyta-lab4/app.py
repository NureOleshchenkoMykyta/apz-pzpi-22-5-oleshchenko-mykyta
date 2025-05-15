from flask import Flask, jsonify, request, redirect, session, Response
from flasgger import Swagger
from functools import wraps
from flask_cors import CORS
from sqlalchemy import create_engine, text
from models import Base
import os
from database import execute_query

# --- Налаштування бази даних ---
DB_NAME = os.getenv("DB_NAME", "postgres")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "12345")
DB_HOST = os.getenv("DB_HOST", "db")  # Docker-сумісний
DB_PORT = os.getenv("DB_PORT", "5432")

SQLALCHEMY_DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(SQLALCHEMY_DATABASE_URL)

# --- Створення схеми analysisstate ---
with engine.connect() as connection:
    connection.execute(text("CREATE SCHEMA IF NOT EXISTS analysisstate"))
    connection.commit()

# --- Створення таблиць згідно моделей ---
Base.metadata.create_all(bind=engine)

# --- Flask ---
app = Flask(__name__)
CORS(app, origins=["http://localhost:5173"], supports_credentials=True)
app.secret_key = '123456789'

# --- Swagger ---
app.config['SWAGGER'] = {
    'title': 'API Documentation',
    'uiversion': 3,
    'version': '1.0.0',
    'description': 'Документація для API'
}

swagger = Swagger(app, config={
    "headers": [],
    "specs": [
        {
            "endpoint": 'apispec_1',
            "route": '/apispec_1.json',
            "rule_filter": lambda rule: True,
            "model_filter": lambda tag: True,
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/apidocs/"
})

def role_required(roles):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            role = request.headers.get('Role')
            if isinstance(roles, list):
                if role not in roles:
                    return jsonify({"message": "Доступ заборонено"}), 403
            else:
                if role != roles:
                    return jsonify({"message": "Доступ заборонено"}), 403
            return func(*args, **kwargs)
        return wrapper
    return decorator


@app.route('/')
def index():
    return redirect('/docs')

@app.route('/docs')
def docs():
    return redirect('/apidocs')

@app.route('/register', methods=['POST'])
def register():
    """
    register
    ---
    tags:
      - Account
    operationId: "Реєстрація нового акаунта"
    parameters:
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            email:
              type: string
              description: Електронна пошта користувача
              example: "example@gmail.com"
            password:
              type: string
              description: Пароль користувача
              example: "password123"
            name:
              type: string
              description: Ім'я користувача
              example: "John Doe"
    responses:
      200:
        description: Акаунт успішно створено
      400:
        description: Невірний запит
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    email = data.get('email')
    password = data.get('password')
    name = data.get('name')

    if not email or not password or not name:
        return jsonify({"error": "Поля email, password, and name повинні бути заповнені"}), 400

    query = """
    INSERT INTO analysisstate.account ("Email", "Password", "Name")
    VALUES (%s, %s, %s);
    """
    params = (email, password, name)

    execute_query(query, params)
    result = execute_query(query, params)
    if result is None:
        print("Запит виконаний успішно, але дані не повернуто.")
    else:
        print("Результат запиту:", result)
    return jsonify({"message": "Акаунт успішно створено"}), 200



# Логін користувача
@app.route('/login', methods=['POST'])
def login():
    """
    login
    ---
    tags:
      - Account
    operationId: "Логін користувача"
    parameters:
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            email:
              type: string
              description: Електронна пошта користувача
              example: "example@gmail.com"
            password:
              type: string
              description: Пароль користувача
              example: "password123"
    responses:
      200:
        description: Логін успішний
      400:
        description: Неправильний email або пароль
    """
    data = request.get_json()
    print("DEBUG: Отримано дані від клієнта:", data)
    email = data.get('email')
    password = data.get('password')
    print("DEBUG: Email:", email, "Password:", password)
    query = "SELECT \"AccountID\", \"Role\" FROM analysisstate.account WHERE \"Email\" = %s AND \"Password\" = %s;"
    account = execute_query(query, (email, password), fetchone=True)
    print("DEBUG: Результат SQL:", account)

    if account:
        account_id, role = account
        session['user_id'] = account_id
        session['role'] = role
        session['email'] = email
        return jsonify({"message": "Вхід успішний", "role": role}), 200
    else:
        return jsonify({"message": "Неправильний email чи пароль"}), 400

# Вийти з акаунту
@app.route('/logout', methods=['POST'])
def logout():
    """
    logout
    ---
    tags:
      - Account
    operationId: "Вихід з системи користувача"
    responses:
      200:
        description: Вихід успішний
      400:
        description: Помилка виходу
    """
    session.clear()

    return jsonify({"message": "Ви вийшли з системи"}), 200


# Отримати всі аккаунти
@app.route('/accounts', methods=['GET'])
@role_required('admin')
def get_all_accounts():
    """
    get_all_accounts
    ---
    tags:
      - Account
    operationId: "Отримати всі акаунти"
    responses:
      200:
        description: Список акаунтів
        schema:
          type: array
          items:
            type: object
            properties:
              AccountID:
                type: integer
              Email:
                type: string
              Name:
                type: string
    """
    query = "SELECT \"AccountID\", \"Email\", \"Name\", \"Role\" FROM analysisstate.account;"
    accounts = execute_query(query)

    return jsonify(accounts), 200


# Видалення акаунта по e-mail
@app.route('/accounts', methods=['DELETE'])
@role_required('admin')
def delete_account():
    """
    delete_account
    ---
    tags:
      - Account
    operationId: "Видалення акаунта по e-mail"
    parameters:
      - name: email
        in: query
        type: string
        required: true
        description: E-mail акаунта для видалення
    responses:
      200:
        description: Акаунт успішно видалено
      400:
        description: Помилка при видаленні акаунта
    """
    email = request.args.get('email')
    if not email:
        return jsonify({"message": "Не вказаний e-mail"}), 400

    account_id_result = execute_query(
        "SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;", (email,))
    if not account_id_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400
    account_id = account_id_result[0][0]

    execute_query("DELETE FROM analysisstate.results WHERE \"AccountID\" = %s;", (account_id,))
    execute_query("DELETE FROM analysisstate.notes WHERE \"AccountID\" = %s;", (account_id,))
    execute_query("DELETE FROM analysisstate.account WHERE \"AccountID\" = %s;", (account_id,))

    return jsonify({"message": "Акаунт і пов'язані дані успішно видалено"}), 200

# Зміна паролю
@app.route('/accounts/<string:email>/change-password', methods=['PUT'])
@role_required('client')
def change_password(email):
    """
    change_password
    ---
    tags:
      - Account
    operationId: "Зміна пароля акаунта"
    parameters:
      - name: email
        in: path
        required: true
        type: string
        description: Email акаунта
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            old_password:
              type: string
              required: true
              description: Старий пароль
            new_password:
              type: string
              required: true
              description: Новий пароль
    responses:
      200:
        description: Пароль успішно змінено
      400:
        description: Невірний старий пароль
    """
    data = request.get_json()
    old_password = data.get('old_password')
    new_password = data.get('new_password')

    # Перевірка старого паролю
    query = "SELECT * FROM analysisstate.account WHERE \"Email\" = %s AND \"Password\" = %s;"
    account = execute_query(query, (email, old_password))

    if not account:
        return jsonify({"message": "Неправильний старий пароль"}), 400

    # Оновлення паролю
    query = "UPDATE analysisstate.account SET \"Password\" = %s WHERE \"Email\" = %s;"
    execute_query(query, (new_password, email))

    return jsonify({"message": "Пароль успішно змінено"}), 200


def determine_emotional_state(stress_level):
    """
    Функція для визначення емоційного стану по рівню стресу.
    """
    if stress_level <= 20:
        return "Спокійний"
    elif 21 <= stress_level <= 40:
        return "Незначний"
    elif 41 <= stress_level <= 60:
        return "Середній"
    elif stress_level >= 61:
        return "Тривожний"
    return "Невідомий"  # На всяк випадок, якщо рівень стресу вийде за межі

@app.route('/results', methods=['POST'])
@role_required(['admin', 'client'])
def create_result():
    """
    create_result
    ---
    tags:
      - Results
    operationId: "Додати результат аналізу"
    parameters:
      - name: email
        in: query
        required: true
        type: string
        description: E-mail акаунта для додавання результату
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            stress_level:
              type: integer
              description: Рівень стресу
              example: 75
    responses:
      200:
        description: Результат успішно додано
      400:
        description: Помилка або акаунт не знайдено
    """
    email = request.args.get('email')  # <-- тільки з query
    if not email:
        return jsonify({"message": "Email обов’язковий"}), 400

    data = request.get_json()
    stress_level = data.get('stress_level')
    if stress_level is None:
        return jsonify({"message": "Рівень стресу є обов'язковим"}), 400

    emotional_state = determine_emotional_state(stress_level)
    account_id_result = execute_query("SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;", (email,))
    if not account_id_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400
    account_id = account_id_result[0][0]

    query = """
    INSERT INTO analysisstate.results ("AccountID", "StressLevel", "EmotionalState")
    VALUES (%s, %s, %s);
    """
    execute_query(query, (account_id, stress_level, emotional_state))
    return jsonify({"message": "Результат успішно додано"}), 200


#Отримання результатів для акаунта
@app.route('/results', methods=['GET'])
@role_required(['admin', 'client'])
def get_results():
    """
    get_results
    ---
    tags:
      - Results
    operationId: "Отримати результати для акаунта"
    parameters:
      - name: email
        in: query
        required: true
        type: string
        description: E-mail акаунта
    responses:
      200:
        description: Список результатів
        schema:
          type: array
          items:
            type: object
            properties:
              ResultID:
                type: integer
              AnalysisDate:
                type: string
              StressLevel:
                type: integer
              EmotionalState:
                type: string
    """
    email = request.args.get('email')
    if not email:
        return jsonify({"message": "Email обов’язковий"}), 400

    account_id_result = execute_query(
        "SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;",
        (email,)
    )
    if not account_id_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400

    account_id = account_id_result[0][0]

    query = """
    SELECT "ResultID", "AnalysisDate", "StressLevel", "EmotionalState"
    FROM analysisstate.results
    WHERE "AccountID" = %s;
    """
    rows = execute_query(query, (account_id,))

    results = []
    for row in rows:
        results.append({
            "ResultID": row[0],
            "AnalysisDate": row[1],
            "StressLevel": row[2],
            "EmotionalState": row[3]
        })

    return jsonify(results), 200


#Оновити результат аналізу
@app.route('/results', methods=['PUT'])
@role_required('client')
def update_result():
    """
    update_result
    ---
    tags:
      - Results
    operationId: "Оновити результат аналізу"
    parameters:
      - name: email
        in: query
        required: true
        type: string
        description: E-mail акаунта
      - name: result_id
        in: query
        required: true
        type: integer
        description: ID результату
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            stress_level:
              type: integer
              description: Новий рівень стресу
              example: 85
    responses:
      200:
        description: Результат успішно оновлено
    """
    email = request.args.get('email')
    result_id = request.args.get('result_id')
    data = request.get_json()
    stress_level = data.get('stress_level')
    if stress_level is None:
        return jsonify({"message": "Рівень стресу є обов'язковим"}), 400

    emotional_state = determine_emotional_state(stress_level)
    account_id_result = execute_query("SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;", (email,))
    if not account_id_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400
    account_id = account_id_result[0][0]

    query = """
    UPDATE analysisstate.results
    SET "StressLevel" = %s, "EmotionalState" = %s
    WHERE "ResultID" = %s AND "AccountID" = %s;
    """
    execute_query(query, (stress_level, emotional_state, result_id, account_id))
    return jsonify({"message": "Результат успішно оновлено"}), 200


#Видалити результат аналізу
@app.route('/results', methods=['DELETE'])
@role_required('client')
def delete_result():
    """
    delete_result
    ---
    tags:
      - Results
    operationId: "Видалити результат аналізу"
    parameters:
      - name: email
        in: query
        required: true
        type: string
        description: E-mail акаунта
      - name: result_id
        in: query
        required: true
        type: integer
        description: ID результату
    responses:
      200:
        description: Результат успішно видалений
    """
    email = request.args.get('email')
    result_id = request.args.get('result_id')
    account_id_result = execute_query("SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;", (email,))
    if not account_id_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400
    account_id = account_id_result[0][0]

    query = "DELETE FROM analysisstate.results WHERE \"ResultID\" = %s AND \"AccountID\" = %s;"
    execute_query(query, (result_id, account_id))
    return jsonify({"message": "Результат успішно видалений"}), 200


#Створити нотатку
@app.route('/notes', methods=['POST'])
@role_required(['client', 'admin'])
def create_note():
    """
    create_note
    ---
    tags:
      - Notes
    operationId: "Створити нотатку"
    parameters:
      - name: Email
        in: header
        required: true
        type: string
        description: Email користувача
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            text:
              type: string
              description: Текст нотатки
              example: "Це моя нотатка"
    responses:
      200:
        description: Нотатка успішно створена
      400:
        description: Помилка при створенні нотатки
    """
    data = request.get_json()
    text = data.get('text')
    email = request.headers.get('Email')

    if not email or not text:
        return jsonify({"message": "Email і текст нотатки обов’язкові"}), 400

    account_result = execute_query("SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;", (email,))
    if not account_result:
        return jsonify({"message": "Акаунт не знайдено"}), 400

    account_id = account_result[0][0]

    query = "INSERT INTO analysisstate.notes (\"AccountID\", \"Text\") VALUES (%s, %s);"
    execute_query(query, (account_id, text))
    return jsonify({"message": "Нотатка успішно створена"}), 200



# Отримати всі нотатки для акаунта
@app.route('/accounts/notes', methods=['GET'])
@role_required(['admin', 'client'])
def get_notes():
    """
    get_notes
    ---
    tags:
      - Notes
    operationId: "Отримати всі нотатки для акаунта"
    parameters:
      - name: email
        in: query
        type: string
        required: true
        description: E-mail акаунта
    responses:
      200:
        description: Список нотаток
        schema:
          type: array
          items:
            type: object
            properties:
              NoteID:
                type: integer
              CreationDate:
                type: string
              Text:
                type: string
    """
    email = request.args.get('email')
    if not email:
        return jsonify({"message": "Email обов’язковий"}), 400

    account_result = execute_query(
        "SELECT \"AccountID\" FROM analysisstate.account WHERE \"Email\" = %s;",
        (email,)
    )
    if not account_result:
        return jsonify({"message": "Акаунт не знайдено"}), 404

    account_id = account_result[0][0]

    query = """
    SELECT "NoteID", "CreationDate", "Text"
    FROM analysisstate.notes
    WHERE "AccountID" = %s;
    """
    rows = execute_query(query, (account_id,))

    notes = []
    for row in rows:
        notes.append({
            "NoteID": row[0],
            "CreationDate": row[1],
            "Text": row[2]
        })

    return jsonify(notes), 200



#Оновити нотатку
@app.route('/notes/<int:note_id>', methods=['PUT'])
@role_required(['admin', 'client'])
def update_note(note_id):
    """
    update_note
    ---
    tags:
      - Notes
    operationId: "Оновити нотатку"
    parameters:
      - name: note_id
        in: path
        required: true
        type: integer
        description: ID нотатки
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            text:
              type: string
              description: Новий текст нотатки
              example: "Оновлений текст нотатки"
    responses:
      200:
        description: Нотатка успішно оновлена
    """
    data = request.get_json()
    text = data.get('text')

    query = "UPDATE analysisstate.notes SET \"Text\" = %s WHERE \"NoteID\" = %s;"
    execute_query(query, (text, note_id))
    return jsonify({"message": "Нотатка успішно оновлена"}), 200

@app.route('/notes', methods=['PUT'])
@role_required(['client'])
def update_own_note():
    """
    Оновити останню нотатку користувача
    ---
    tags:
      - Notes
    parameters:
      - name: email
        in: query
        required: true
        type: string
      - name: body
        in: body
        required: true
        schema:
          type: object
          properties:
            text:
              type: string
              description: Новий текст нотатки
    responses:
      200:
        description: Нотатка оновлена
    """
    data = request.get_json()
    new_text = data.get('text')
    email = request.args.get('email')

    if not new_text or not email:
        return jsonify({"message": "Invalid input"}), 400

    account = execute_query(
        'SELECT "AccountID" FROM analysisstate.account WHERE "Email" = %s',
        (email,), fetchone=True
    )
    if not account:
        return jsonify({"message": "Користувача не знайдено"}), 404

    account_id = account[0]

    update_query = """
    UPDATE analysisstate.notes
    SET "Text" = %s
    WHERE "NoteID" = (
        SELECT "NoteID"
        FROM analysisstate.notes
        WHERE "AccountID" = %s
        ORDER BY "CreationDate" DESC
        LIMIT 1
    )
    """
    execute_query(update_query, (new_text, account_id))
    return jsonify({"message": "Нотатка оновлена"}), 200


# Отримати інформацію про користувача
@app.route('/user-info', methods=['GET'])
@role_required(['admin', 'client'])
def get_user_info():
    """
    Отримання інформації про користувача
    ---
    tags:
      - User
    parameters:
      - name: email
        in: query
        required: true
        type: string
    responses:
      200:
        description: Дані користувача
    """
    email = request.args.get('email')
    if not email:
        return jsonify({'error': 'Email обовʼязковий'}), 400

    # Отримати користувача
    account_query = """
    SELECT "AccountID", "Name", "Email"
    FROM analysisstate.account
    WHERE "Email" = %s
    """
    account = execute_query(account_query, (email,), fetchone=True)
    if not account:
        return jsonify({'error': 'Користувача не знайдено'}), 404

    account_id, name, email = account

    # Отримати останній стан
    result_query = """
    SELECT "EmotionalState"
    FROM analysisstate.results
    WHERE "AccountID" = %s
    ORDER BY "AnalysisDate" DESC
    LIMIT 1
    """
    result = execute_query(result_query, (account_id,), fetchone=True)
    emotional_state = result[0] if result else '—'

    # Отримати останню нотатку
    note_query = """
    SELECT "Text"
    FROM analysisstate.notes
    WHERE "AccountID" = %s
    ORDER BY "CreationDate" DESC
    LIMIT 1
    """
    note = execute_query(note_query, (account_id,), fetchone=True)
    note_text = note[0] if note else '—'

    return jsonify({
        'email': email,
        'name': name,
        'state': emotional_state,
        'note': note_text
    })

@app.route('/user-results', methods=['GET'])
@role_required(['admin', 'client'])
def get_user_results():
    email = request.args.get('email')
    query = """
        SELECT r."AnalysisDate", r."EmotionalState", r."StressLevel"
        FROM analysisstate.results r
        JOIN analysisstate.account a ON r."AccountID" = a."AccountID"
        WHERE a."Email" = %s
        ORDER BY r."AnalysisDate" DESC
    """
    results = execute_query(query, (email,))

    # Сформуємо правильний JSON
    return jsonify([
        {
            "date": row[0].isoformat(),  # або str(row[0])
            "state": row[1],
            "stress": int(row[2]) if row[2] is not None else 0
        }
        for row in results
    ])


#Видалити нотатку
@app.route('/notes/<int:note_id>', methods=['DELETE'])
@role_required(['admin', 'client'])
def delete_note(note_id):
    """
    delete_note
    ---
    tags:
      - Notes
    operationId: " "
    parameters:
      - name: note_id
        in: path
        required: true
        type: integer
        description: ID нотатки
    responses:
      200:
        description: Нотатка успішно видалена
    """
    query = "DELETE FROM analysisstate.notes WHERE \"NoteID\" = %s;"
    execute_query(query, (note_id,))
    return jsonify({"message": "Нотатка успішно видалена"}), 200

@app.route('/backup', methods=['GET'])
@role_required('admin')
def backup_data():
    """
    backup_data
    ---
    tags:
      - Backup
    operationId: "Створити резервну копію всіх даних"
    responses:
      200:
        description: JSON файл з даними
    """
    import json
    from flask import Response

    def fetch_all(query):
        return execute_query(query)

    accounts = fetch_all("SELECT * FROM analysisstate.account;")
    results = fetch_all("SELECT * FROM analysisstate.results;")
    notes = fetch_all("SELECT * FROM analysisstate.notes;")

    backup = {
        "accounts": accounts,
        "results": results,
        "notes": notes
    }

    backup_json = json.dumps(backup, default=str, ensure_ascii=False, indent=2)

    return Response(
        backup_json,
        mimetype='application/json',
        headers={"Content-Disposition": "attachment;filename=backup.json"}
    )

@app.route('/restore', methods=['POST'])
@role_required('admin')
def restore_data():
    """
    restore_data
    ---
    tags:
      - Backup
    operationId: "Відновити дані з резервної копії"
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            accounts:
              type: array
            results:
              type: array
            notes:
              type: array
    responses:
      200:
        description: Відновлення завершено
    """
    data = request.get_json()

    if not data:
        return jsonify({"message": "Немає даних для імпорту"}), 400

    try:
        # Очистити таблиці перед імпортом
        execute_query("DELETE FROM analysisstate.notes;")
        execute_query("DELETE FROM analysisstate.results;")
        execute_query("DELETE FROM analysisstate.account;")

        for acc in data.get('accounts', []):
            execute_query("""
                INSERT INTO analysisstate.account("AccountID", "Email", "Password", "Name", "Role")
                VALUES (%s, %s, %s, %s, %s);
            """, tuple(acc))

        for res in data.get('results', []):
            execute_query("""
                INSERT INTO analysisstate.results("ResultID", "AccountID", "AnalysisDate", "StressLevel", "EmotionalState")
                VALUES (%s, %s, %s, %s, %s);
            """, tuple(res))

        for note in data.get('notes', []):
            execute_query("""
                INSERT INTO analysisstate.notes("NoteID", "AccountID", "CreationDate", "Text")
                VALUES (%s, %s, %s, %s);
            """, tuple(note))

        return jsonify({"message": "Дані успішно імпортовано"}), 200

    except Exception as e:
        return jsonify({"message": f"Помилка імпорту: {str(e)}"}), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
