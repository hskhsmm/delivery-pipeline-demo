# Delivery Pipeline Demo

[Delivery Pipeline](https://github.com/hskhsmm/delivery-pipeline)의 Jenkins Shared Library를 실제 서비스 저장소에서 호출하는 Java 21 / Gradle 샘플입니다. `GET /health`는 HTTP 200과 `ok`를 반환합니다.

## 로컬 실행

```sh
./gradlew --no-daemon build
java -Dservice.port=18080 -jar build/libs/service.jar
```

다른 터미널에서 `curl -i http://127.0.0.1:18080/health`를 실행합니다.

## Jenkins 연결

1. 기존 로컬 환경을 사용할 때는 `docker start delivery-publish-engine`을 실행한 뒤 `docker start delivery-jenkins-publish`를 실행합니다. Jenkins는 `http://localhost:8081`에서 열립니다.
2. Jenkins의 공유 라이브러리 이름을 `delivery-pipeline`로 설정하고, Git 소스를 `https://github.com/hskhsmm/delivery-pipeline.git`으로 지정합니다. [공유 라이브러리 설정 가이드](https://github.com/hskhsmm/delivery-pipeline/blob/main/docs/getting-started.md)
3. 새 **Pipeline from SCM** 잡에서 Git URL은 `https://github.com/hskhsmm/delivery-pipeline-demo.git`, 브랜치는 `*/main`, Script Path는 `Jenkinsfile`을 선택합니다. 이 저장소를 HTTPS로 체크아웃할 수 있는 GitHub 읽기 자격 증명을 Jenkins에 등록하고 SCM의 Credentials에서 선택합니다. [Jenkins Git 자격 증명 안내](https://plugins.jenkins.io/git/#using-credentials)
4. `java` 라벨 에이전트에서 JDK 21과 Docker Buildx를 사용할 수 있는지 확인합니다. `jenkins.yml`의 `dockerhub-publisher`는 Docker Hub 발행 자격 증명 ID입니다.
5. **Build Now**를 실행한 뒤 `Checkout → Configure → Verify → Publish Image`가 통과하는지 확인합니다. Jenkins의 `published-image.json`에서 `sourceRevision`이 서비스 커밋과 일치하는지 확인하고, `image`의 digest를 GitOps 저장소에 반영합니다.

`Dockerfile`은 로컬 Jenkins 발행 검증에 사용한 경량 Java 런타임 구성을 포함합니다. `jenkins.yml`의 이미지 목적지는 공개 Docker Hub 데모 저장소입니다.
