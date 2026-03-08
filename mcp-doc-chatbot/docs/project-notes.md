# Project Alpha — Tech Notes

## Architecture
- Backend: Spring Boot 3.3, PostgreSQL
- Frontend: React + Vite + Tailwind
- Infra: Kubernetes on GKE

## Key Design Decisions
- Use event-sourcing for audit trail
- JWT auth, 1h expiry, refresh tokens in httpOnly cookies
- All API calls go through the API Gateway for rate limiting

## Known Issues
- Search is full-table-scan right now — needs indexing (tracked in #42)
- Image uploads > 5MB fail in dev — S3 config issue

## Links
- Repo: https://github.com/example/project-alpha
- Staging: https://staging.example.com
