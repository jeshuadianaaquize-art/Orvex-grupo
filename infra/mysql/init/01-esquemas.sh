#!/bin/bash
# Crea un esquema y un usuario por microservicio (arquitectura del informe, punto 5.3.1.12).
set -euo pipefail

ESQUEMAS="usuarios catalogo compatibilidad optimizacion inventario pedidos notificaciones auditoria reportes"

SQL=""
for e in $ESQUEMAS; do
  SQL+="CREATE DATABASE IF NOT EXISTS orvex_${e} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  SQL+="CREATE USER IF NOT EXISTS 'orvex_${e}'@'%' IDENTIFIED BY '${DB_PASSWORD}';"
  SQL+="GRANT ALL PRIVILEGES ON orvex_${e}.* TO 'orvex_${e}'@'%';"
done
SQL+="FLUSH PRIVILEGES;"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "$SQL"
echo "Orvex: esquemas y usuarios creados: ${ESQUEMAS}"
