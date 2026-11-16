# Delivery Pipeline Demo

[Delivery Pipeline](https://github.com/hskhsmm/delivery-pipeline)의 Jenkins Shared Library를 실제 서비스 저장소에서 호출하는 Java 21 / Gradle 샘플입니다. `GET /health`로 준비 상태를 확인하고, `GET /shipping-quote`로 주문 금액에 따른 배송비를 계산합니다.

## 로컬 실행

```sh
./gradlew --no-daemon build
java -Dservice.port=18080 -jar build/libs/service.jar
```

다른 터미널에서 API를 호출합니다.

```sh
curl -i 'http://127.0.0.1:18080/health'
curl -i 'http://127.0.0.1:18080/shipping-quote?subtotal=29999'
curl -i 'http://127.0.0.1:18080/shipping-quote?subtotal=30000'
```

`subtotal`은 원 단위로 최대 아홉 자리의 숫자를 받습니다. 30,000원 미만이면 배송비 3,000원을 더하고, 그 이상이면 배송비를 0원으로 계산합니다. 금액이 없거나 형식이 잘못됐으면 HTTP 400을 돌려줍니다.

```json
{"subtotal":29999,"shippingFee":3000,"total":32999}
```

`./gradlew build`는 `httpTest`도 실행합니다. 실제 HTTP 요청으로 배송비 경계값, 잘못된 입력, HTTP 메서드와 `/health` 응답을 검증합니다.

## Jenkins 연결

1. 기존 로컬 환경을 사용할 때는 `docker start delivery-publish-engine`을 실행한 뒤 `docker start delivery-jenkins-publish`를 실행합니다. Jenkins는 `http://localhost:8081`에서 열립니다.
2. Jenkins의 공유 라이브러리 이름을 `delivery-pipeline`로 설정하고, Git 소스를 `https://github.com/hskhsmm/delivery-pipeline.git`으로 지정합니다. [공유 라이브러리 설정 가이드](https://github.com/hskhsmm/delivery-pipeline/blob/main/docs/getting-started.md)
3. 로컬 Jenkins의 `delivery-github-demo` 잡이 이 저장소에 연결되어 있습니다. 새 Jenkins에서 설정할 때는 **Pipeline from SCM**의 Git URL을 `git@github.com:hskhsmm/delivery-pipeline-demo.git`, 브랜치를 `*/main`, Script Path를 `Jenkinsfile`로 지정합니다. 테스트 레포에 읽기 전용 배포 키를 추가하고, 대응하는 개인 키를 Jenkins의 **SSH Username with private key** 자격 증명으로 등록합니다. 로컬 잡은 사용자명 `git`, 자격 증명 ID `delivery-demo-github-readonly`를 사용합니다. Jenkins에서 GitHub SSH 호스트 키도 검증하도록 설정합니다. [GitHub 배포 키](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) · [Jenkins SSH 호스트 키 검증](https://plugins.jenkins.io/git-client/#ssh-host-key-verification)
4. `java` 라벨 에이전트에서 JDK 21과 Docker Buildx를 사용할 수 있는지 확인합니다. `jenkins.yml`의 `dockerhub-publisher`는 Docker Hub 발행 자격 증명 ID입니다.
5. **Build Now**를 실행한 뒤 `Checkout → Configure → Verify → Publish Image → Open GitOps PR`이 통과하는지 확인합니다. Jenkins의 `published-image.json`에서 `sourceRevision`과 이미지 digest를 확인하고, 같은 digest를 담은 GitOps PR을 검토해 병합합니다. Argo CD가 병합 커밋을 자동 적용하면 두 API를 호출해 배포를 확인합니다.

`Dockerfile`은 로컬 Jenkins 발행 검증에 사용한 경량 Java 런타임 구성을 포함합니다. `jenkins.yml`의 이미지 목적지는 공개 Docker Hub 데모 저장소입니다.


## 검증 결과

로컬 Jenkins의 `delivery-github-demo #3`이 서비스 커밋 `01c4f84`와 공유 라이브러리 커밋 `6a3367c`를 GitHub에서 직접 체크아웃해 이미지를 발행했습니다. 발행된 digest를 로컬 GitOps 저장소에 직접 커밋한 뒤 Argo CD가 `delivery-sandbox`의 기존 배포를 갱신했고, Service의 `/health`에서 HTTP 200과 `ok`를 확인했습니다.
