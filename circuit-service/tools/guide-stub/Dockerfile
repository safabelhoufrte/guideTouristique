FROM python:3.13-slim
WORKDIR /app
COPY server.py .
USER 65534:65534
EXPOSE 8001
CMD ["python", "-u", "server.py", "--host", "0.0.0.0", "--port", "8001"]
