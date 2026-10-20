# Guía de Despliegue en AWS EC2 - Pedidos360

Esta guía detalla paso a paso cómo desplegar la plataforma **Pedidos360** en Amazon Web Services (AWS) utilizando dos instancias EC2 separadas:
1. **EC2 Broker:** Broker RabbitMQ en contenedor Docker aislado.
2. **EC2 Aplicación:** BFF, Microservicios (`ms-catalog`, `ms-orders`, `ms-notify`) y Base de Datos PostgreSQL.

---

## 1. Arquitectura en AWS

```mermaid
flowchart TD
    subgraph VPC["AWS VPC (Default o Personalizada)"]
        subgraph Subnet["Subnet Pública (ej: 172.31.0.0/20)"]
            subgraph EC2_App["Instancia EC2: App y DB"]
                direction TB
                BFF["BFF API Gateway (:8088)"]
                MS_Cat["ms-pedidos360-catalog (:8086)"]
                MS_Ord["ms-pedidos360-orders (:8087)"]
                MS_Not["ms-pedidos360-notify (Consumidor)"]
                Postgres["PostgreSQL DB (:5432)"]
            end

            subgraph EC2_Rabbit["Instancia EC2: RabbitMQ"]
                direction TB
                RabbitMQ["RabbitMQ Broker (:5672)"]
                RabbitUI["Management UI (:15672)"]
            end
        end
    end

    InternetClients["Cliente / Postman"] -->|HTTP :8088| BFF
    GmailServer["Google SMTP (smtp.gmail.com:587)"] <--|STARTTLS| MS_Not
    MS_Ord -->|AMQP :5672 (IP Privada)| RabbitMQ
    MS_Not -->|AMQP :5672 (IP Privada)| RabbitMQ
```

---

## 2. Creación de Instancias EC2 y Security Groups

### 2.1 Especificaciones de las Instancias
- **AMI:** Amazon Linux 2023 (AL2023) o Amazon Linux 2 (AL2).
- **Usuario SSH por defecto:** `ec2-user` (en lugar de `ubuntu`).
- **Tipo de Instancia:**
  - **EC2 RabbitMQ:** `t3.micro` o `t3.small` (suficiente para RabbitMQ en Alpine).
  - **EC2 Aplicación:** `t3.small` o `t3.medium` (recomendado para 4 microservicios Java 17 + PostgreSQL).
- **Almacenamiento (EBS):** 20 a 30 GB gp3 en la EC2 de la Aplicación.

### 2.2 Configuración de Security Groups

#### A. Security Group para RabbitMQ (`sg-pedidos360-rabbitmq`)
| Tipo | Protocolo | Puerto | Origen (Source) | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| **SSH** | TCP | `22` | `Tu IP Pública / 32` | Acceso seguro por terminal |
| **Custom TCP** | TCP | `5672` | `sg-pedidos360-app` (o IP Privada EC2 App) | Tráfico AMQP entre microservicios y RabbitMQ |
| **Custom TCP** | TCP | `15672` | `Tu IP Pública / 32` | Consola Web de Administración RabbitMQ |

#### B. Security Group para la Aplicación (`sg-pedidos360-app`)
| Tipo | Protocolo | Puerto | Origen (Source) | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| **SSH** | TCP | `22` | `Tu IP Pública / 32` | Acceso seguro por terminal |
| **Custom TCP** | TCP | `8088` | `0.0.0.0/0` (o tu IP) | Acceso al BFF para Postman / Frontend |
| **Outbound Rules** | Todo | Todos | `0.0.0.0/0` | Salida para descarga de paquetes, conexión a RabbitMQ y Gmail SMTP (:587) |

> [!IMPORTANT]
> Anota la **IP Privada** de la instancia EC2 de RabbitMQ (ej: `172.31.24.150`). Los microservicios de la EC2 de la Aplicación deben comunicarse usando esta IP interna para evitar costos de transferencia y maximizar la velocidad.

### 2.3 Asignación de IP Elástica (Elastic IP) y Conexión con API Manager

- **¿Debe tener IP Elástica la EC2 de la Aplicación (BFF)? SÍ, es indispensable.**
  - **Motivo:** El API Manager (AWS API Gateway, Azure API Management, Kong, Apigee, etc.) necesita una URL de backend estática y confiable (ej: `http://<IP-ELASTICA>:8088`). Si utilizas la IP pública dinámica estándar de EC2, cada vez que detengas o reinicies la instancia la IP cambiará y romperá todas las rutas y configuraciones del API Manager.
- **¿Debe tener IP Elástica la EC2 de RabbitMQ? Generalmente NO.**
  - **Motivo:** La comunicación entre la Aplicación y RabbitMQ es **completamente interna** y ocurre a través de la **IP Privada** (`172.31.X.X`) dentro de la misma VPC. La IP privada nunca cambia mientras la máquina exista (incluso si la apagas y la prendes). Exponer RabbitMQ con una IP pública estática además incrementa costos innecesarios y riesgos de seguridad.
  - *(Solo requeriría IP elástica si necesitas acceder a la consola web `:15672` siempre por la misma URL pública desde fuera de AWS).*

#### Pasos para asignar la IP Elástica a la Aplicación en la Consola de AWS:
1. En la consola de AWS, dirígete a **EC2** -> **Network & Security** -> **Elastic IPs**.
2. Haz clic en **Allocate Elastic IP address** y confirma con **Allocate** (región estándar).
3. Selecciona la IP generada, haz clic en **Actions** -> **Associate Elastic IP address**.
4. En **Resource type**, selecciona **Instance**, escoge la instancia de tu **Aplicación (EC2 App)** y haz clic en **Associate**.
5. ¡Listo! Esa IP fija será la que configures en tu **API Manager** y en tu Postman en el puerto `8088`.

---

## 3. Instalación de Docker y Docker Compose en Amazon Linux (En Ambas EC2)

Conéctate por SSH a cada instancia usando el usuario **`ec2-user`**:
```bash
ssh -i tu-llave.pem ec2-user@<IP-PUBLICA-EC2>
```

### 3.1 Si usas Amazon Linux 2023 (AL2023 - Más común / Recomendado):
```bash
# 1. Actualizar el sistema e instalar Docker y Git desde los repositorios oficiales de AWS
sudo dnf update -y
sudo dnf install -y docker git

# 2. Iniciar y habilitar el servicio de Docker para que arranque con la máquina
sudo systemctl enable --now docker

# 3. Agregar ec2-user al grupo docker para ejecutar comandos sin 'sudo'
sudo usermod -aG docker ec2-user

# 4. Instalar el plugin oficial de Docker Compose (v2)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 5. Aplicar los cambios de grupo a la sesión actual (o sal y vuelve a entrar por SSH)
newgrp docker
```

### 3.2 Si usas Amazon Linux 2 (AL2):
```bash
# 1. Actualizar e instalar Docker mediante amazon-linux-extras
sudo yum update -y
sudo amazon-linux-extras install docker -y
sudo yum install -y git

# 2. Iniciar y habilitar Docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# 3. Instalar Docker Compose (binario standalone)
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 4. Aplicar cambios de grupo
newgrp docker
```

### 3.3 Verificar la instalación:
```bash
docker --version
docker compose version
```

---

## 4. Despliegue en la EC2 de RabbitMQ

1. En la EC2 de RabbitMQ, crea un directorio para el proyecto:
   ```bash
   mkdir -p ~/pedidos360-rabbitmq && cd ~/pedidos360-rabbitmq
   ```

2. Copia el archivo `docker-compose.rabbitmq.yml` a la instancia desde tu equipo local:
   ```bash
   scp -i tu-llave.pem docker-compose.rabbitmq.yml ec2-user@<IP-PUBLICA-EC2-RABBITMQ>:~/pedidos360-rabbitmq/
   ```

3. Levanta el contenedor de RabbitMQ:
   ```bash
   docker compose -f docker-compose.rabbitmq.yml up -d
   ```

4. Aplica la política de **Dead Letter Exchange (DLX)** ejecutando el siguiente comando:
   ```bash
   docker exec -it pedidos360-rabbitmq rabbitmqctl set_policy DLX_POLICY "^q\.cmd\." '{"dead-letter-exchange":"cmd.dead.dlx"}' --apply-to queues
   ```

5. Comprueba el estado:
   ```bash
   docker compose -f docker-compose.rabbitmq.yml ps
   ```
   *Acceso al panel web: `http://<IP-PUBLICA-EC2-RABBITMQ>:15672` (usuario: `guest`, contraseña: `guest`).*

---

## 5. Despliegue en la EC2 de la Aplicación

1. En tu máquina local, copia el repositorio / archivos del proyecto hacia la EC2 de la Aplicación usando `scp` con el usuario `ec2-user`:
   ```bash
   # En Windows PowerShell o Git Bash (desde D:\Escritorio\CloudComputing):
   scp -i "C:\ruta\tu-llave.pem" -r . ec2-user@<IP-PUBLICA-EC2-APP>:~/CloudComputing
   ```
   *(O alternativamente, clona tu repositorio Git directamente en la EC2 con `git clone <tu-repo>`).*

2. Conéctate a la EC2 de la Aplicación:
   ```bash
   ssh -i tu-llave.pem ec2-user@<IP-PUBLICA-EC2-APP>
   cd ~/CloudComputing
   ```

3. Crea y edita tu archivo de entorno `.env`:
   ```bash
   cp .env.example .env
   nano .env
   ```
   Asegúrate de colocar los valores correspondientes:
   ```properties
   # IP Privada interna de tu EC2 de RabbitMQ:
   RABBITMQ_HOST=172.31.X.X
   RABBITMQ_PORT=5672
   RABBITMQ_USERNAME=guest
   RABBITMQ_PASSWORD=guest

   # Configuración de Gmail con contraseña de aplicación de 16 caracteres:
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_AUTH=true
   SMTP_STARTTLS=true
   SMTP_USERNAME=tu_correo@gmail.com
   SMTP_PASSWORD=abcd efgh ijkl mnop
   MAIL_FROM=tu_correo@gmail.com
   MAIL_SENDER_NAME=Pedidos360 Café

   # Base de datos y seguridad:
   DB_USER=postgres
   DB_PASSWORD=postgres
   SECURITY_ENABLED=false
   ```

4. Compila y levanta la pila de la aplicación:
   ```bash
   docker compose -f docker-compose.app.yml up -d --build
   ```

5. Inspecciona los logs para verificar el inicio de los contenedores:
   ```bash
   docker compose -f docker-compose.app.yml logs -f
   ```
   *Verás que `ms-pedidos360-catalog` detecta la base de datos vacía y ejecuta automáticamente el seeder insertando los 12 productos de cafetería de especialidad.*

---

## 6. Pruebas End-to-End con Postman

1. Abre **Postman** en tu equipo local e importa la colección `Pedidos360.postman_collection.json`.
2. En las variables de la colección, cambia el valor de `bff_url`:
   ```
   bff_url: http://<IP-PUBLICA-EC2-APP>:8088
   ```
3. Ejecuta las peticiones de prueba:
   - **`1.2 Listar Productos de Catálogo`**: Verás los 12 productos de café precargados (Espresso, Latte, Capuccino, Cold Brew, granos Geisha, prensas francesas, etc.).
   - **`2.1 Crear Pedido`**: Envía el JSON con tu propio correo en `clienteEmail` y `productoId: 1`, `cantidad: 2`.
     - *Resultado:* El stock disminuye automáticamente en catálogo y recibes el correo de confirmación con la boleta PDF adjunta en tu Gmail.
   - **`2.5 Actualizar Estado a DESPACHADO`**:
     - *Resultado:* Recibes un segundo correo de notificación informando que tu pedido fue despachado y va en camino.

---

## 7. Comandos de Diagnóstico y Monitoreo Útiles

- **Ver estado de los contenedores:**
  ```bash
  docker compose -f docker-compose.app.yml ps
  ```
- **Ver logs en tiempo real de un servicio específico:**
  ```bash
  docker logs -f pedidos360-notify
  docker logs -f pedidos360-orders
  docker logs -f pedidos360-catalog
  docker logs -f bff-api
  ```
- **Verificar conectividad de red con RabbitMQ desde la EC2 App:**
  ```bash
  nc -zv <IP-PRIVADA-RABBITMQ> 5672
  ```
- **Reiniciar un servicio tras cambiar credenciales en `.env`:**
  ```bash
  docker compose -f docker-compose.app.yml up -d --force-recreate ms-notify
  ```
