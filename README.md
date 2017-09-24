# k-raider

> This is the Kotlin version

## ?

## Build

```bash
mvn clean package
```

## Run

```bash
startPort=9090

for i in `seq 1 10`;
do
    resultat=$(($startPort+$i))
    REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="R" SERVICE_PORT=$resultat PORT=$resultat java  -jar target/k-raider-1.0-SNAPSHOT-fat.jar&
done
```


## Kill the raiders

```bash
kill `awk 'BEGIN{for(i=980;i<=995;i++){print i}}'`
```


