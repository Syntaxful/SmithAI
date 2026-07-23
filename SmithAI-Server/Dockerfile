FROM python:3.11-slim

WORKDIR /app

# Install build dependencies for llama-cpp-python
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py ./
COPY config.yml ./
COPY README.md ./
COPY models ./models

# Port 8000 is used by the FastAPI server
EXPOSE 8000

ENV SMITHAI_CONFIG=/app/config.yml

CMD ["python", "app.py"]
