# Ejecutar Machly con Docker

## Primera ejecución

1. Copia `.env.docker.example` como `.env`.
2. Cambia `JWT_SECRET` por una cadena larga y aleatoria.
3. Ejecuta:

```powershell
docker compose up -d --build
docker compose ps
```

Abre `http://localhost:3000`. La documentación móvil estará en `http://localhost:3000/api/docs`.

## Datos persistentes

La base SQLite vive en el volumen Docker `machly_data`. Actualizar o recrear el contenedor no borra los datos.

```powershell
docker volume inspect machly_data
```

No ejecutes `docker compose down -v` salvo que quieras borrar definitivamente toda la base.

## Operación

```powershell
# Estado
docker compose ps

# Logs
docker compose logs -f machly

# Reiniciar
docker compose restart machly

# Detener sin borrar datos
docker compose down
```

## Acceso móvil

El puerto `3000` está publicado en todas las interfaces de la computadora. Usa `http://IP-DE-LA-PC:3000/api/` desde un teléfono conectado a la misma red Wi‑Fi.

En producción se recomienda colocar un proxy HTTPS delante del contenedor y usar un secreto JWT administrado fuera del repositorio.
