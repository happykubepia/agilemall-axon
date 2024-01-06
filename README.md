# Microservice Pattern Example using Axon framwork

source code를 보시려면  [여기](https://github.com/happykubepia/agilemall-axon.git)를 누르세요. 

## 사전준비
- IntelliJ 설치: multi module and gradle project를 만들기 위해서 IntelliJ 필요
- Docker설치
  - Mac:
    - brew install docker
    - open -a docker
  - Window: https://happycloud-lee.tistory.com/14

- MySQL설치
  - 설치
  ```
  % docker run -d --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=P@ssw0rd$ -v ~/data/mysql:/var/lib/mysql mysql
  ```
  - DB생성
  https://happycloud-lee.tistory.com/229 : 4.보안설정부터 참고
  ```
    % docker exec -it mysql sh
    # mysql -u root -p 
    Enter password: 
    mysql> create database orderDB;
    mysql> create database paymentDB;
    mysql> create database deliveryDB;
    mysql> create database inventoryDB;
    mysql> create database reportDB;
    mysql> show databases;
    mysql> exit
    # exit
  ```
  - DBeaver에서 연결: 'Driver properties'탭에서 'allowPublicKeyRetrieval'을 'true'로 변경해야 함

- Axon Server 설치
  - 설치
```  
docker run -d --rm --name axonserver -p 18024:8024 -p 18124:8124 -e axoniq.axonserver.devmode.enabled=true axoniq/axonserver
```
  - Web console접근: http://localhost:18024

----

## 테스트 

ㅇ Build jar
application 최상위 디렉토리에서 수행
```
./gradlew clean :{server name}:buildNeeded [--stacktrace --info] --refresh-dependencies -x test 
```

ㅇ 테스트: http://localhost:18080/swagger-ui/index.html
```
{
  "userId": "hiondal",
  "orderReqDetails": [
    {
      "productId": "PROD_10041",
      "orderSeq": 1,
      "qty": 10
    },
    {
      "productId": "PROD_10042",
      "orderSeq": 2,
      "qty": 5
    },
    {
      "productId": "PROD_10043",
      "orderSeq": 3,
      "qty": 15
    }
  ],
  "paymentReqDetails": [
    {
      "paymentGbcd": "10",
      "paymentRate": 0.9
    },
    {
      "paymentGbcd": "20",
      "paymentRate": 0.1
    }
  ]
}
```

----

## Trouble shooting

> Project build 시 'Task classes not found in root project'라는 에러 발생시 
  - 'File > Invalidated Caches' 수행하여 Cache 삭제 후 재시작 하면 됨 
  - 참고) https://dlrudtn108.tistory.com/39


> 에러 발생 후 Order의 EventHandler가 실행 안될 때 
  원인: Order와 다른 서비스의 token index값이 일치 하지 않기 때문임 
  - orders, order_detail의 모든 record 삭제 
  ```
  delete  from orderDB.order_detail;
  delete from orderDB.orders;
  ```
  - Token Global index 초기화 
    - Axon server UI에서 [Reset Event Store] 클릭하여 token 초기화
    - 각 DB의 token_entry 테이블의 데이터 삭제
    ```
    delete from orderDB.token_entry;
    delete from paymentDB.token_entry;
    delete from deliveryDB.token_entry;
    ``` 

> 에러 발생 후 Saga transaction이 모두 실행 되었는데 Payment, Delivery의 Event handler가 실행 안될 때 
  - 원인: Order와 다른 서비스의 token index값이 일치 하지 않기 때문임 
  - 각 DB의 token_entry 테이블의 데이터 삭제. 삭제 하면 자동으로 Last token index값으로 일치 시킴
    ```
    delete from orderDB.token_entry;
    delete from paymentDB.token_entry;
    delete from deliveryDB.token_entry;
    ```
  - Token값 확인 
  ```
  select cast(token as char) from orderDB.token_entry;
  select cast(token as char) from paymentDB.token_entry;
  select cast(token as char) from deliveryDB.token_entry;
  ```

> AXONIQ-2000 에러
Error occurred during store batch operation: 
io.axoniq.axonserver.exception.MessagingPlatformException: [AXONIQ-2000] Invalid sequence number 0 for aggregate
  - 원인1) Aggregate class의 @AggregateIdentifier로 지정한 property가 유일한 값이 아니면 발생 
    - 설명: 각 Aggregate객체는 구별할 수 있는 유일한 id가 있어야 함   
    - 조치: @AggregateIdentifier로 지정한 property는 유일한 값이 저장되도록 정확히 지정 

  - 원인2) CommandGateway 객체가 transient로 생성되지 않은 경우 
    - 설명: transient 키워드는 해당 객체를 직렬화(저장 또는 전송을 위해 binary형식으로 변환하는 것)하지 않게하는 예약 키워드임. CommandGateway객체로 전송 시 binary형식으로 변환하지 않아야 하기 때문에 반드시 붙여야 함
    - 예)
    @Autowired
    private transient CommandGateway commandGateway;


> 참고
  - MySQL 데이터 디렉토리 찾기 
```
mysql> show variables like 'datadir';
+---------------+-----------------+
| Variable_name | Value           |
+---------------+-----------------+
| datadir       | /var/lib/mysql/ |
+---------------+---------------
```



