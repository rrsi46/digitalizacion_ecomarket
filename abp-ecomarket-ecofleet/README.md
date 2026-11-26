# ABP Digitalización — Ecomarket (AD) + Ecofleet (PSP)

## Entrega 1: Cloud Computing e Infraestructura

Infraestructura contenerizada con Docker Compose compuesta por:
- **ecomarket-db**: Contenedor MariaDB 10.11 con volumen persistente (`ecomarket_db_data`) y carga automática del esquema (`db/init.sql`).
- **ecofleet-psp**: Microservicio Java/Maven de logística (`AlmacenAutomatizado`) compilado con build multi-etapa.
- **ecomarket-net**: Red privada virtualizada (`bridge`) que comunica ambos servicios.

## Despliegue en un comando
```bash
docker compose up -d --build