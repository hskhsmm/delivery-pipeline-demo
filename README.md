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
3. 로컬 Jenkins의 `delivery-github-demo` 잡이 이 저장소에 연결되어 있습니다. 새 Jenkins에서 설정할 때는 **Pipeline from SCM**의 Git URL을 `git@github.com:hskhsmm/delivery-pipeline-demo.git`, 브랜치를 `*/main`, Script Path를 `Jenkinsfile`로 지정합니다. 테스트 레포에 읽기 전용 배포 키를 추가하고, 대응하는 개인 키를 Jenkins의 **SSH Username with private key** 자격 증명으로 등록합니다. 로컬 잡은 사용자명 `git`, 자격 증명 ID `delivery-demo-github-readonly`를 사용합니다. Jenkins에서 GitHub SSH 호스트 키도 검증하도록 설정합니다. [GitHub 배포 키](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) · [Jenkins SSH 호스트 키 검증](https://plugins.jenkins.io/git-client/#ssh-host-key-verification)
4. `java` 라벨 에이전트에서 JDK 21과 Docker Buildx를 사용할 수 있는지 확인합니다. `jenkins.yml`의 `dockerhub-publisher`는 Docker Hub 발행 자격 증명 ID입니다.
5. **Build Now**를 실행한 뒤 `Checkout → Configure → Verify → Publish Image`가 통과하는지 확인합니다. Jenkins의 `published-image.json`에서 `sourceRevision`이 서비스 커밋과 일치하는지 확인하고, `image`의 digest를 GitOps 저장소에 반영합니다.

`Dockerfile`은 로컬 Jenkins 발행 검증에 사용한 경량 Java 런타임 구성을 포함합니다. `jenkins.yml`의 이미지 목적지는 공개 Docker Hub 데모 저장소입니다.


## 검증 결과

로컬 Jenkins의 `delivery-github-demo #3`이 서비스 커밋 `01c4f84`와 공유 라이브러리 커밋 `6a3367c`를 GitHub에서 직접 체크아웃해 이미지를 발행했습니다. 발행된 digest를 로컬 GitOps 저장소에 직접 커밋한 뒤 Argo CD가 `delivery-sandbox`의 기존 배포를 갱신했고, Service의 `/health`에서 HTTP 200과 `ok`를 확인했습니다.
