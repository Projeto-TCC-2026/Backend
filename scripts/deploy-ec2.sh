#!/bin/sh
# Executado na EC2 pelo SSM (como root) a cada deploy.
set -eu

JAR=/opt/tccapp/app.jar

# 1. Baixa o JAR novo do S3 (o aws via snap fica em /snap/bin)
/snap/bin/aws s3 cp s3://tcc-backend-artifacts-2026/backend/app.jar "$JAR.new" --region us-east-1

# 2. Guarda a versão atual para rollback e troca pela nova
cp "$JAR" "$JAR.prev"
mv "$JAR.new" "$JAR"
chown tccapp:tccapp "$JAR"
systemctl restart tccapp

# 3. Espera a API responder (até 2 minutos)
for i in $(seq 1 24); do
  if curl -fs -o /dev/null http://localhost:8080/swagger-ui/index.html; then
    echo "Deploy OK: API no ar."
    exit 0
  fi
  sleep 5
done

# 4. Não subiu: volta a versão anterior e marca o deploy como falho
echo "API nao subiu em 2 minutos. Restaurando a versao anterior."
cp "$JAR.prev" "$JAR"
chown tccapp:tccapp "$JAR"
systemctl restart tccapp
exit 1