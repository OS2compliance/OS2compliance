# Docker image vejledning

## Digital-identity
### Til produktion
Brug os2compliance-deploy repo

## Via pipeline

Pipelinen bygger og pusher automatisk et Docker image til ECR når du opretter et git tag.

```bash
git tag 2026-03-07
git push origin 2026-03-07
```

> Brug dags dato som tag i formatet `YYYY-MM-DD`.

Pipelinen bruger tagget som image-tag, så det færdige image bliver:
```
${AWS_ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/os2compliance:2026-03-07
```

---

## Andre

### Krav
- Java 21 (Amazon Corretto anbefales)
- Maven Wrapper (`./mvnw`) – ligger i roden af projektet
- Docker
- AWS CLI med adgang til ECR

### 1. Byg projektet
```bash
./mvnw clean package -DskipTests
```

### 2. Log ind i ECR
```bash
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com
```

### 3. Byg Docker image
```bash
docker build . -t ${AWS_ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/os2compliance:2026-03-07
```

### 4. Push til ECR
```bash
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.eu-west-1.amazonaws.com/os2compliance:2026-03-07
```

> Udskift datoen `2026-03-07` med dags dato i formatet `YYYY-MM-DD`.