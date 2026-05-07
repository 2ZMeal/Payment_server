## 📂 Eureka Server (Service Discovery)

각 서비스의 위치(IP, Port)를 등록하고 관리하는 Service Discovery 서버입니다.
모든 서비스는 실행 시 이 서버에 자신의 정보를 등록하며, 서비스 간 통신 시 이 서버를 통해 서로를 찾습니다.

## 📍 인프라 정보

- 표준 포트: 8761

- 관리 대시보드: http://localhost:8761
    - 현재 어떤 서비스가 살아있는지 실시간으로 확인할 수 있습니다.

## 🛠️ 클라이언트 서비스 설정 (Eureka Client)

각 마이크로서비스에서 Eureka Server에 자신을 등록하려면 아래 설정을 추가해야 합니다.

### 1. 의존성 추가 (build.gradle)

```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

### 2. application.yml 설정

project-configs/configs/common/application.yml에 정의된 아래 설정을 모든 서비스에 배달합니다. (별도로 설정할 필요 없음)

```yml
eureka:
  instance:
    prefer-ip-address: true
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

따라서 각 서비스의 내부(src/main/resources/application.yml)에는 "어디서 설정을 가져올지"만 적어주면 됩니다.

```yml
# user-service 프로젝트 내부의 application.yml 예시
spring:
  application:
    name: user-service
  config:
    import: "optional:configserver:http://localhost:8888"
```

## 🚀 실행 및 연동 가이드

### 1. 실행 순서

아래 순서대로 서비스를 실행하세요.

1. **Infra**: Docker 컨테이너 (DB 등)

2. **Eureka Server**: 본 프로젝트 (가장 먼저 실행되어야 함)

3. **Config Server**: 설정을 배달하기 위해 Eureka에 먼저 등록됨

4. **Microservices**: `user-service`, `order-service` 등

### 2. 서비스 등록 확인 (대시보드)

서버 실행 후 대시보드(http://localhost:8761)에 접속하여 Instances currently registered with Eureka 목록을 확인하세요.

- CONFIG-SERVER: UP (1) 상태여야 함

- USER-SERVICE: UP (1) 상태여야 함
