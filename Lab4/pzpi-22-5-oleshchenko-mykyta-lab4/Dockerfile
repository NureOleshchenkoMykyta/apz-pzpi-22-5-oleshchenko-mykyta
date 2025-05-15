FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .

# 🛠 Встановлюємо залежності ОС і pip-пакети
RUN apt-get update && apt-get install -y libpq-dev gcc && \
    pip install --no-cache-dir -r requirements.txt && \
    apt-get remove -y gcc && apt-get autoremove -y

COPY . .

EXPOSE 5000

CMD ["python", "app.py"]
