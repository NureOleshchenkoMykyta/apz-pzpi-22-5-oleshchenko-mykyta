import psycopg2
from psycopg2 import sql
import os

def create_connection():
    try:
        connection = psycopg2.connect(
            dbname=os.getenv("DB_NAME", "postgres"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", "12345"),
            host=os.getenv("DB_HOST", "localhost"),
            port=os.getenv("DB_PORT", "5432")
        )
        return connection
    except Exception as e:
        print(f"Помилка підключення: {e}")
        return None

def execute_query(query, params=None, fetchone=False):
    connection = create_connection()
    if connection:
        cursor = connection.cursor()
        try:
            cursor.execute(query, params)
            if query.strip().lower().startswith('select'):
                return cursor.fetchone() if fetchone else cursor.fetchall()
            connection.commit()
            return None
        except Exception as e:
            print(f"Помилка виконання запиту: {e}")
        finally:
            cursor.close()
            connection.close()


def close_connection(connection):
    if connection:
        connection.close()

def test_connection():
    connection = create_connection()
    if connection:
        print("Підключення з базою даних встановлено.")
        connection.close()
    else:
        print("Не вдалося підключитися до бази даних.")

def test_database():
    try:
        connection = psycopg2.connect(
            dbname=os.getenv("DB_NAME", "postgres"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", "12345"),
            host=os.getenv("DB_HOST", "localhost"),
            port=os.getenv("DB_PORT", "5432")
        )
        cursor = connection.cursor()
        cursor.execute("SELECT * FROM analysisstate.account LIMIT 1;")
        print("Таблиця account знайдена.")
        connection.close()
    except Exception as e:
        print(f"Помилка: {e}")


# test_database()