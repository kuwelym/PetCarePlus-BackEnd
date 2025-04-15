### Edit the content in the  `.env` in the root directory:
For the `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` you need to use a Gmail account to send emails. You can create an app password for your Gmail account [here](https://myaccount.google.com/apppasswords).
```ini
POSTGRES_DB=<POSTGRES_DB>
POSTGRES_PASSWORD=<POSTGRES_PASSWORD>
POSTGRES_USER=<POSTGRES_USER>
REDIS_PASSWORD=<REDIS_PASSWORD>
REDIS_HOST=<REDIS_HOST>
HTTP_USER=<HTTP_USER>
HTTP_PASSWORD=<HTTP_PASSWORD>

# Application
SPRING_MAIL_USERNAME=<YOUR_EMAIL>
SPRING_MAIL_PASSWORD=<YOUR_GMAIL_APP_PASSWORD>

# JWT secret key 
APPLICATION_SECURITY_JWT_SECRET_KEY=<APPLICATION_SECURITY_JWT_SECRET_KEY>

# VNPAY 
VNPAY_MERCHANT_ID=
VNPAY_SECRET_KEY=

```

### Run project
If you run with docker, you should stop the services that are using the ports `5432`(PostgreSQL) and `6379`(Redis) before running the project.

```bash
docker-compose up -d
```
