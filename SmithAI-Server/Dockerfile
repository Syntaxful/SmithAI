FROM python:3.11-slim

WORKDIR /app

# Install build dependencies for llama-cpp-python (fallback only; wheels are preferred).
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py ./
COPY config.yml ./
COPY README.md ./
COPY download_model.py ./
COPY models ./models

EXPOSE 8000

ENV SMITHAI_CONFIG=/app/config.yml

CMD ["python", "app.py"]
